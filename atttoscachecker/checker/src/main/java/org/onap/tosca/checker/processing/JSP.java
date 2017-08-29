/*
 * Copyright (c) 2017 <AT&T>.  All rights reserved.
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
package org.onap.tosca.checker.processing;

import java.io.FileReader;
import java.io.IOException;
import java.io.File;

import java.net.URI;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.script.Invocable;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.AbstractJSObject;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.Report;
import org.onap.tosca.checker.Construct;
import org.onap.tosca.checker.Facet;


/**
 * Java Script Processor
 * Each script is represented by a Target and the JSP processor maintains a collection of Targets, i.e. scripts.
 * A collection of targets can be used with only one JSP processor at a time (as the processor stores processor specific * compiled versions within the target). 
 */
public class JSP implements Processor<JSP> {

  private ScriptEngine engine;
	private Collection<? extends Target> targets;

  public JSP(String[] theScripts) {
		this(Arrays.stream(theScripts)
								.map(s -> new Target(s, new File(s).toURI()))
								.collect(Collectors.toList()));
	}
  
	public JSP(File[] theScripts) {
		this(Arrays.stream(theScripts)
								.map(s -> new Target(s.getName(), s.toURI()))
								.collect(Collectors.toList()));
	}
	
	public JSP(URI[] theScripts) {
		this(Arrays.stream(theScripts)
								.map(s -> new Target(s.toString(), s))
								.collect(Collectors.toList()));
	}

	/**
   * The given collection is allowed to change while used by the JSP engine but access to it needs to be synchronized.
   * The engine uses the target field of each Target to store a compiled version of each script. An external reset of
   * this field (maybe in order to indicate some change in the Target) will caue a re-compilation of the Target.
   */
	public JSP(Collection<? extends Target> theTargets) {
		this.targets = theTargets;
    ScriptEngineManager engineManager = new ScriptEngineManager();
    this.engine = engineManager.getEngineByName("nashorn");
	}

	public Collection<? extends Target> targets() {
		return this.targets;
	}

	/* pre-compiles all known targets
   */
	protected void compile() throws ProcessorException {
		synchronized (this.targets) {
			for (Target t: this.targets)
				compile(t);
		}
	}

	protected CompiledScript compile(Target theTarget) throws ProcessorException {

		CompiledScript cs = null;

		synchronized(theTarget) {		
			try {
				cs = (CompiledScript)theTarget.getTarget();
			}
			catch(ClassCastException ccx) {
				throw new ProcessorException(theTarget, "Unexpected target content");
			}

			if (cs == null) {
				try {
					cs = ((Compilable)this.engine).compile(theTarget.open());
					theTarget.setTarget(cs);
				}
  	  	catch (IOException iox) {
					throw new ProcessorException(theTarget, "Failed to read script", iox);
				}
				catch (ScriptException sx) {
					throw new ProcessorException(theTarget, "Failed to compile script", sx);
				}
			}
		}

		return cs;
	}

	public ContextBuilder process(Catalog theCatalog) {
		return new ContextBuilder(
											this.engine.createBindings())
											//new DelegateBindings(this.engine.getBindings(ScriptContext.ENGINE_SCOPE)))
								.with("catalog", new JSCatalog(theCatalog));
	}

	/**
   */
	public class ContextBuilder implements ProcessBuilder<JSP> { 

		private ScriptContext		context;

		protected ContextBuilder(Bindings theBindings) {
			this.context = new SimpleScriptContext();
			this.context.setBindings(theBindings, Process.PROCESS_SCOPE /*ScriptContext.ENGINE_SCOPE*/);
		}

		public ContextBuilder withPreprocessing(BiFunction<Target, ScriptContext, Boolean> thePreprocessing) {
			this.context.setAttribute("preprocessor", thePreprocessing, Process.PROCESS_SCOPE);
			return this;
		}

		public ContextBuilder withPostprocessing(BiFunction<Target, ScriptContext, Boolean> thePostprocessing) {
			this.context.setAttribute("postprocessor", thePostprocessing, Process.PROCESS_SCOPE);
			return this;
		}

		public ContextBuilder with(String theName, Object theValue) {
			this.context.getBindings(Process.PROCESS_SCOPE).put(theName, theValue);
			return this;
		}
		
		public ContextBuilder withOpt(String theName, Object theValue) {
			if (theValue != null)
				this.context.getBindings(Process.PROCESS_SCOPE).put(theName, theValue);
			return this;
		}

		public JSProcess process() {
 			return new JSProcess(this.context);
		}

	}

  /**
   */
  public class JSProcess implements Process<JSP> {

		private Report											report = new Report();
		private Iterator<? extends Target> 	scripts;
		private JScriptInfo									scriptInfo = new JScriptInfo();
		private	Target											script; //script currently being evaluated
		private boolean											stopped = false;
		private ScriptContext								context;

		private JSProcess(ScriptContext theContext) {

			this.context = theContext;
			this.context.getBindings(Process.PROCESS_SCOPE)
											.put("stop", new Consumer<String>() {
																			public void accept(String theMsg) {
																				JSProcess.this.stopped = true;
																				//log the message??
																			}
																		});
			this.context.getBindings(Process.PROCESS_SCOPE)
											.put("report", new Consumer<String>() {
																			public void accept(String theMsg) {
																				JSProcess.this.report.add(new ProcessorException(script, theMsg));
																			}
																		});
			this.context.getBindings(Process.PROCESS_SCOPE)
											.put("reportOnce", new Consumer<String>() {
																			public void accept(String theMsg) {
																				JSProcess.this.report.addOnce(new ProcessorException(script, theMsg));
																			}
																		});
			this.scripts = JSP.this.targets.iterator();
		}

		protected String infoName(Target theTarget) {
			String name = theTarget.getName();
			return name.substring(0, name.indexOf(".")) + "_info";
		}

		public JSP processor() {
			return JSP.this;
		}

		public boolean hasNext() {
			return !this.stopped && this.scripts.hasNext();
		}

		protected Target next() {
			if (hasNext())
				return this.script = this.scripts.next();
			else
				throw new RuntimeException("Process is completed");
		}

		protected boolean runProcessor(String theName) throws ProcessorException {
			BiFunction<Target, ScriptContext, Boolean> proc = (BiFunction<Target, ScriptContext, Boolean>)
																														this.context.getAttribute(theName, Process.PROCESS_SCOPE);
			if (proc != null) {
				try {
					return proc.apply(this.script, this.context).booleanValue();
				}
				catch (Exception x) {
					throw new ProcessorException(this.script, theName + "failed", x);
				}
			}

			return true;
		}

		public Process runNext() throws ProcessorException {
			Target target = next();
			synchronized(target) {
				String name = infoName(target);
				try {
					if (runProcessor("preprocessor")) {
						compile(target).eval(this.context);
						runProcessor("postprocessor");
					}
				}
				catch (ScriptException sx) {
					throw new ProcessorException(target, "Failed to execute validation script", sx);
				}
			}
			
			return this;
		}
		
		public Process runNextSilently() {
			try {
				return runNext();
			}
			catch (ProcessorException px) {
				this.report.add(px);
			}
			return this;
		}

		public Report run() {
			while (hasNext())
				runNextSilently();
			return this.report;
		}

		public void stop() {
			this.stopped = true;
		}

		public Report report() {
			return this.report;
		}
	}

	private static class JScriptInfo implements TargetInfo {
	
		private JSObject info;

		protected JScriptInfo() {
		}

		protected JScriptInfo setInfo(JSObject theInfo) {
			this.info = theInfo;
			return this;
		}

		public Set<String>	entryNames() {
			return this.info == null ? Collections.EMPTY_SET : this.info.keySet();
		}

		public boolean	hasEntry(String theName) {
			return this.info == null ? false : this.info.hasMember(theName);
		}

		public Object	getEntry(String theName) {
			return this.info == null ? null : 
								 this.info.hasMember(theName) ?	this.info.getMember(theName) : null;
		}
	}


  /* Exposes the catalog information in a more Java Script friendly manner.
   */
  public static class JSCatalog {

		private Catalog catalog;

		private JSCatalog(Catalog theCatalog) {
			this.catalog = theCatalog;
		}
		
		/** */
    public JSTarget[] targets() {
			return 
				this.catalog.targets()
					.stream()
					.map(t -> { return new JSTarget(t); })
					.toArray(size -> new JSTarget[size]); //or toArray(JSNode[]::new)
    }
    
		public JSTarget[] topTargets() {
			return 
				this.catalog.topTargets()
					.stream()
					.map(t -> { return new JSTarget(t); })
					.toArray(size -> new JSTarget[size]); //or toArray(JSNode[]::new)
    }

		/** */
    public String[] types(String theConstruct) {
			Set<String> names = 
							this.catalog.getConstructTypes(Enum.valueOf(Construct.class,theConstruct)).keySet();
      return names.toArray(new String[names.size()]);
    }

		/** */
		public boolean isDerivedFrom(String theConstruct, String theType, String theSuperType) {
			return this.catalog.isDerivedFrom(Enum.valueOf(Construct.class,theConstruct), theType, theSuperType);
		}

		/** */
		public JSObject facetDefinition(String theConstruct, String theType, String theFacet, String theName) {
			return new JSElement(theName,
													 this.catalog.getFacetDefinition(
															Enum.valueOf(Construct.class, theConstruct), theType, 
															Enum.valueOf(Facet.class, theFacet), theName));
		}


		/** */
/*
    public JSElement[] targetNodes(Target theTarget) {
			return 
				this.catalog.getTargetTemplates(theTarget, Construct.Node)
					.entrySet()
						.stream()
						.map(e -> { return new JSElement(e.getKey(),e.getValue()); })
						.toArray(size -> new JSElement[size]); //or toArray(JSNode[]::new)
		}
*/

		public class JSTarget {

			private Target 				tgt;
	  	private	JXPathContext jxPath;

			private JSTarget(Target theTarget) {
				this.tgt = theTarget;
				this.jxPath = JXPathContext.newContext(this.tgt.getTarget());
				this.jxPath.setLenient(true);
			}

			public String getName() { return this.tgt.getName(); }

			public JSElement resolve(String thePath) {
				Object res = jxPath.getValue(thePath);
				if (res instanceof Map) {
					return new JSElement(thePath, (Map)res);
				}
				//??
				return null;
			}

			public JSElement[] getInputs() {
				
				Map<String,Map> inputs = (Map<String,Map>)jxPath.getValue("/topology_template/inputs");
				return (inputs == null) ? 
										new JSElement[0]
									: inputs.entrySet()
											.stream()
											.map(e -> { return new JSElement(e.getKey(),e.getValue()); })
											.toArray(size -> new JSElement[size]);
			}

//			public JSElement[] getOutputs() {
//			}

      public JSElement getMetadata() {
				return new JSElement("metadata", (Map)jxPath.getValue("/metadata"));
			}

			public JSElement[] getNodes() {
				return 
					JSCatalog.this.catalog.getTargetTemplates(this.tgt, Construct.Node)
						.entrySet()
							.stream()
							.map(e -> { return new JSElement(e.getKey(),e.getValue()); })
							.toArray(size -> new JSElement[size]); //or toArray(JSElement[]::new)
			}

//			public JSElement[] getPolicies() {
//			}

		}


		/*
		 */
  	public class JSElement extends AbstractJSObject {
	

			private String 	name;
  	  private Map			def;

			private JSElement(String theName, Object theDef) {
//System.out.println("JSObject " + theName + ":" + theDef);
				this.name = theName;
    	  this.def = theDef == null ? Collections.emptyMap() 
																	: (theDef instanceof Map) ? (Map)theDef
																														: Collections.singletonMap("value",theDef);
			}

			public String getName() { return this.name; }

			public boolean hasMember(String theMember) {
//	System.out.println("hasMember " + theMember);
				return this.def.containsKey(theMember); 
			}

			public Object getMember(final String theMember) {
				Object val = this.def.get(theMember);
//	System.out.println("getMember " + theMember + ": " + val);
				if (val != null) {
					if (val instanceof Map) {
						return new JSElement(theMember, val);
					/*
						return ((Map<String,?>)obj).entrySet()
											.stream()
											.map((Map.Entry<String,?> e) -> { return new JSElement(e.getKey(),e.getValue()); })
											.toArray(size -> new JSElement[size]);
					*/
					}
					
					if (val instanceof List) {
						//a property value can be a list of: primitive types or maps (for a user defined type)
						//requirements are exposed as a list of maps
						List lval = (List)val;
						if (lval.get(0) instanceof Map) {
							return lval
											.stream()
											.map((e) -> new JSElement(theMember, e))
											.toArray(size -> new JSElement[size]);
							
						/*
							return val
											.stream()
											.map((e) -> {
																		System.out.println("Found in " + theMember + ": " + e); 
																		Map.Entry<String,?> re = ((Map<String,?>)e).entrySet().iterator().next();
																		return new JSElement(re.getKey(), re.getValue());
																	})
											.toArray(size -> new JSElement[size]);
						*/
						}
					}
					
					return val;
				}
				else {
					if ("name".equals(theMember))
						return this.name;
					if ("toString".equals(theMember))
						return _toString;
					if ("hasOwnProperty".equals(theMember))
						return _hasOwnProperty;	
//		System.out.println("Could not find " + theMember + " in " + this.def);
					return super.getMember(theMember);
				}
			}		
			/* TODO: we do not expose 'name' in here */
			public Set<String> keySet() {
//				System.out.println("keySet: " + this.def.keySet());
				return this.def.keySet(); 
			}

/* having problem with getDefaultValue in Jenkins builds */
/*
			public Object getDefaultValue(Class<?> hint) {
//				System.out.println("getDefaultValue: " + hint);
				if (String.class.equals(hint))
					return this.def.toString();

				return super.getDefaultValue(hint);
			}
*/

		}


	static final JSObject _toString = 
						new TracerJSObject("_toString") {
							public Object call(Object thiz, Object... args) {
								return ((JSElement)thiz).def.toString();
							}

							public boolean isFunction() { return true; }
						};

	static final JSObject _hasOwnProperty = 
						new TracerJSObject("_hasOwnProperty") {
							public Object call(Object thiz, Object... args) {
//				System.out.println("_hasOwnProperty: " + args.length);
								return ((JSElement)thiz).def.containsKey(args[0]);
							}

							public boolean isFunction() { return true; }
						};	

	}//JSCatalog



  private static class TracerJSObject extends AbstractJSObject {

		private String mark;
	
		TracerJSObject(String theMark) {
			this.mark = theMark;
		}

		public Object call(Object thiz, Object... args)	{
			System.out.println(this.mark + ":call");
			return super.call(thiz, args);
		}

		public Object newObject(Object... args) {
			System.out.println(this.mark + ":newObject");
			return super.newObject(args);
		}

		public Object eval(String s) {
			System.out.println(this.mark + ":eval");
			return super.eval(s);
		}

		public Object getMember(String name) {
			System.out.println(this.mark + ":getMember");
			return super.getMember(name);
		}

		public Object getSlot(int index) {
			System.out.println(this.mark + ":getSlot");
			return super.getSlot(index);
		}

		public boolean hasMember(String name) {
			System.out.println(this.mark + ":hasMember");
			return super.hasMember(name);
		}

		public boolean hasSlot(int slot) {
			System.out.println(this.mark + ":hasSlot");
			return super.hasSlot(slot);
		}

		public void removeMember(String name) {
			System.out.println(this.mark + ":removeMember");
			super.removeMember(name);
		}

		public void setMember(String name, Object value) {
			System.out.println(this.mark + ":setMember");
			super.setMember(name,value);
		}

		public void setSlot(int index, Object value) {
			System.out.println(this.mark + ":setSlot");
			super.setSlot(index,value);
		}

		public Set<String> keySet() {
			System.out.println(this.mark + "JSObject:keySet");
			return super.keySet();
		}

		public Collection<Object> values() {
			System.out.println(this.mark + ":values");
			return super.values();
		}

		public boolean isInstance(Object instance) {
			System.out.println(this.mark + ":isInstance");
			return super.isInstance(instance);
		}

		public boolean isInstanceOf(Object clazz) {
			System.out.println(this.mark + ":isInstanceOf");
			return super.isInstance(clazz);
		}

		public String getClassName() {
			System.out.println(this.mark + ":getClassName");
			return super.getClassName();
		}

		public boolean isFunction() {
			System.out.println(this.mark + ":isFunction");
			return super.isFunction();
    }

		public boolean isStrictFunction() {
			System.out.println(this.mark + ":isStrictFunction");
			return super.isStrictFunction();
		}

		public boolean isArray() {
			System.out.println(this.mark + ":isArray");
			return super.isArray();
		}

		public Object getDefaultValue(Class<?> hint) {
			System.out.println(this.mark + ":getDefaultValue(" + hint + ")");
			return super.getDefaultValue(hint);
		}
	}

}
