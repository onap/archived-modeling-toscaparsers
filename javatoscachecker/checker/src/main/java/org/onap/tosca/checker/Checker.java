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
package org.onap.tosca.checker;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;

import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.Maps;
import com.google.common.collect.MapDifference;
import com.google.common.reflect.Invokable;

import com.google.common.io.CharStreams;

import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

import kwalify.YamlParser;
import kwalify.Validator;
import kwalify.Rule;
import kwalify.Types;
import kwalify.SchemaException;
import kwalify.SyntaxException;
import kwalify.ValidationException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import org.apache.commons.lang.reflect.ConstructorUtils;

import org.reflections.Reflections;
import org.reflections.util.FilterBuilder;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner; 
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.adapters.JavaReflectionAdapter;

import org.onap.tosca.checker.annotations.Checks;
import org.onap.tosca.checker.annotations.Catalogs;
import org.onap.tosca.checker.annotations.Validates;

import static org.onap.tosca.checker.Messages.Message;


/*
 * To consider: model consistency checking happens now along with validation
 * (is implemented as part of the validation hooks). It might be better to
 * separate the 2 stages and perform all the consistency checking once 
 * validation is completed.
 */
public class Checker {


  public static void main(String[] theArgs) {

    if (theArgs.length == 0) {
      System.err.println("checker resource_to_validate [processor]*");
      return;
    }

		try {
			Catalog cat = Checker.check(new File(theArgs[0]));

			for (Target t: cat.targets()) {
				System.err.println(t.getLocation() + "\n" + cat.importString(t) + "\n" + t.getReport());
			}

			for (Target t: cat.sortedTargets()) {
				System.out.println(t);
			}

		}
		catch (Exception x) {
			x.printStackTrace();
		}
  }


  private Target   						target = null; //what we're validating at the moment
	private	Map<String, Target>	grammars = new HashMap<String, Target>(); //grammars for the different tosca versions

	private CheckerConfiguration config;
	private Catalog   		catalog;
	private	TargetLocator	locator = new CommonLocator();

	private Table<String, Method, Object> handlers = HashBasedTable.create();
	private Messages											messages;
	private Logger    log = Logger.getLogger(Checker.class.getName());


	private static String[] EMPTY_STRING_ARRAY = new String[0];
	private static final	String[] grammarFiles = new String[] {"tosca/tosca_simple_yaml_1_0.grammar",
																															"tosca/tosca_simple_yaml_1_1.grammar"};	


  public Checker(CheckerConfiguration theConfig) throws CheckerException {
		this.config = theConfig;
    loadGrammars();
		loadAnnotations();
		messages = new Messages();
  }

  public Checker() throws CheckerException {
		this(new CheckerConfiguration());
	}

	private void loadGrammars() throws CheckerException {

		for (String grammarFile: this.config.grammars()) {
			Target grammarTarget = this.locator.resolve(grammarFile);
			if (grammarTarget == null) {
				log.warning("Failed to locate grammar " + grammarFile);
				continue;
			}

    	parseTarget(grammarTarget);
			if (grammarTarget.getReport().hasErrors()) {
				log.warning("Invalid grammar " + grammarFile + ": " + grammarTarget.getReport().toString());
				continue;
			}

			List versions = null;
			try {
				versions = (List)
										((Map)
											((Map)
												((Map)grammarTarget.getTarget())
													.get("mapping"))
														.get("tosca_definitions_version"))
															.get("enum");
			}
			catch (Exception x) {
				log.warning("Invalid grammar " + grammarFile + ": cannot locate tosca_definitions_versions");
			}
			if (versions == null || versions.isEmpty()) {
				log.warning("Invalid grammar " + grammarFile + ": no tosca_definitions_version specified");
				continue;
			}

			for (Object version: versions) {
				this.grammars.put(version.toString(), grammarTarget);
			}
		}

		log.finer("Loaded grammars: " + this.grammars);
	}

	private void loadAnnotations() throws CheckerException {

		Reflections				 reflections = new Reflections(
																			new ConfigurationBuilder()
																						.forPackages("org.onap.tosca")
																						.filterInputsBy(new FilterBuilder()
																															.include(".*\\.class")
																														)
																						.setScanners(new TypeAnnotationsScanner(),
																												 new SubTypesScanner(),
																												 new MethodAnnotationsScanner())
																						.setExpandSuperTypes(false)
																						//.setMetadataAdapter(new JavaReflectionAdapter())
																				);

		Map<Class, Object> refs = new HashMap<Class, Object>();
		Set<Method> 			 methods = null; 
		
		//very similar but annotatons cannot be handled in a more 'generic' manner

		methods = reflections.getMethodsAnnotatedWith(Checks.class);
		for (Method method: methods) {
			handlers.put("checks:" + method.getAnnotation(Checks.class).path(),
									 method,
									 refs.computeIfAbsent(method.getDeclaringClass(),	type -> newInstance(type))); 
		}
			
		methods = reflections.getMethodsAnnotatedWith(Catalogs.class);
		for (Method method: methods) {
			handlers.put("catalogs:" + method.getAnnotation(Catalogs.class).path(),
								   method,
								   refs.computeIfAbsent(method.getDeclaringClass(),	type -> newInstance(type)));
		}
		
		methods = reflections.getMethodsAnnotatedWith(Validates.class);
		for (Method method: methods) {
			Validates annotation =  method.getAnnotation(Validates.class);
			handlers.put(annotation.timing() + "-validates:" + annotation.rule(),
								   method,
								   refs.computeIfAbsent(method.getDeclaringClass(),	type -> newInstance(type)));
		}
	}


	private Object newInstance(Class theType) {
		try {
			return (getClass() == theType) ? this
																 	   : theType.newInstance();
		}
		catch(Exception x) {
			throw new RuntimeException(x); 
		}
	}

	/*
	 * Lookup one of the handlers, by handler type
	 */
	public <T> T getHandler(Class<T> theType) {
		return (T)handlers.values()
											.stream()
									 	  .filter(h -> theType.isInstance(h))
									 		.findFirst()
									 		.orElse(null);
	}

	public CheckerConfiguration configuration() {
		return this.config;
	}

	public void setTargetLocator(TargetLocator theLocator) {
		this.locator = theLocator;
	}

	public TargetLocator getTargetLocator() {
		return this.locator;
	}
	
  public Collection<Target> targets() {
		if (this.catalog == null)
			throw new IllegalStateException("targets are only available after check");

		return this.catalog.targets();
	}

	public Catalog catalog() {
		return this.catalog;
	}

	/* a facility for handling all files in a target directory ..
   */
  public static Catalog check(File theSource)
																	throws CheckerException {
		
		Catalog catalog = new Catalog(commonsCatalog());
		Checker	checker = new Checker();
		try {	
			if (theSource.isDirectory()) {
				for (File f: theSource.listFiles()) {
    	  	if (f.isFile()) {
      	  	checker.check(new Target(theSource.getCanonicalPath(), f.toURI().normalize()), catalog);
      		}
    		}
			}
			else {
      	checker.check(new Target(theSource.getCanonicalPath(), theSource.toURI().normalize()), catalog);
 			}
		}
		catch (IOException iox) {
			throw new CheckerException("Failed to initialize target", iox);
		}

		return catalog;
	}
	
 	/**
	 * Main checking process entry point. In this case the source is passed to the locator in order to
 	 * obtain a {@link org.onap.tosca.checker.Target target}, and then all other processing stages are performed.
	 * @param String the string representation of the uri pointing to the document/template to be processed
	 * @throws CheckerException for any checker encountered error 
	 */	
  public void check(String theSource)
																	throws CheckerException {
		check(theSource, buildCatalog());
	}

	/**
	 * Main checking entry point using a pre-computed Catalog. Same as {@link org.onap.tosca.checker.Chacker#check(String) check} except that the given catalog information is available. i.e. all types available in the given catalog types are
available and the available targets won't be re-processed.
	 */
  public void check(String theSource, Catalog theCatalog)
																							throws CheckerException {
		Target tgt =
					this.locator.resolve(theSource);
		if (null == tgt) {
			throw new CheckerException("Unable to locate the target " + theSource);
		}

		check(tgt, theCatalog);
	}

	/**
	 * Starts the processing after the localization phase, i.e. the Target is obtained/constructed outside the checker.
	 * @param Target the Target representation of the document/template to be processed. The actual target content (yaml
	 *					character string) is obtained by calling {@link org.onap.tosca.checker.Target#open() open} on the target 
	 * @throws CheckerException for any checker encountered error 
	 */
	public void check(Target theTarget) throws CheckerException {
		check(theTarget, buildCatalog());
	}

	/**
	 *
	 * @param Target the Target representation of the document/template to be processed. The actual target content (yaml
	 *					character string) is obtained by calling {@link org.onap.tosca.checker.Target#open() open} on the target 
   * @param theCatalog a default catalog providing common construct definitions
	 * @throws CheckerException for any checker encountered error 
	 */
	public void check(Target theTarget, Catalog theCatalog) throws CheckerException {

		this.catalog = theCatalog;
		this.locator.addSearchPath(theTarget.getLocation());
		
		if (this.catalog.addTarget(theTarget, null)) {
			List<Target> targets = parseTarget(theTarget);
			if (theTarget.getReport().hasErrors())
				return;
			for (Target target: targets) {
				this.catalog.addTarget(target, null);
				//what about this -> this.catalog.addTarget(target, theTarget);
				if (!validateTarget(target).getReport().hasErrors()) {
					checkTarget(target);
				}
			}
		}
	}

	/**
	 * Starts the processing after the {@link org.onap.tosca.checker.Staget#parsed parsed} stage. As such the Target must
	 * have been located (content is available) and {@link org.onap.tosca.checker.Staget#parsed parsed} (the parsed form
   * is stored within the Target, {@see org.onap.tosca.checker.Target#getTarget getTarget}).
   * The target will be validated (grammar) and chcked (consistency). While the checker uses snakeyaml to parse
	 * a yaml document using this entry point allows one to use any other yaml parser for a long as it produces a
	 * compatible representation (java primitive types object representations, Strings, Maps and Lists).
 	 * 
   * @param theTarget the processing subject, located and parsed.
	 * @throws CheckerException for any checker encountered error 
	 */
	public void validate(Target theTarget) throws CheckerException {
		validate(theTarget, buildCatalog());
	}

	/**
	 *
   * @param theTarget the processing subject, located and parsed.
   * @param theCatalog a default catalog providing common construct definitions
   * @throws CheckerException 
	 */
	public void validate(Target theTarget, Catalog theCatalog) throws CheckerException {
		this.catalog = theCatalog;
		this.locator.addSearchPath(theTarget.getLocation());
		
		if (this.catalog.addTarget(theTarget, null)) {
			if (!validateTarget(theTarget).getReport().hasErrors()) {
				checkTarget(theTarget);
			}
		}
	}


  /* */
  protected List<Target> parseTarget(final Target theTarget)
																		throws CheckerException {
		log.entering(getClass().getName(), "parseTarget", theTarget);

		Reader source = null;
		try {
			source = theTarget.open();
		}
		catch (IOException iox) {
			throw new CheckerException("Failed to open target " + theTarget, iox);
		}
		

		List<Object> yamlRoots = new ArrayList<Object>();
    try {
			Yaml yaml = new Yaml();
			for (Object yamlRoot: yaml.loadAll(source)) {
				yamlRoots.add(yamlRoot);
			}

			//yamlRoots.add(
			//				new YamlParser(CharStreams.toString(source)).parse());
    }
/*
		catch(SyntaxException sx) {
			System.out.println(sx.getLineNumber() + ": " + sx.getMessage());
			theTarget.report(sx);
		}
*/
    catch(Exception x) {
			theTarget.report(x);
			return Collections.EMPTY_LIST;
			//return Collections.singletonSet(theTarget);
    }
    finally {
      try {
        source.close();
      }
      catch (IOException iox) {
        //just log it
      }
    }

		List<Target> targets = new ArrayList<Target>(yamlRoots.size());
		if (yamlRoots.size() == 1) {
			//he target turned out to be a bare document
			theTarget.setTarget(yamlRoots.get(0));
			theTarget.setStage(Stage.parsed);
			targets.add(theTarget);
		}
		else {	
			//the target turned out to be a stream containing multiple documents
			for (int i = 0; i < yamlRoots.size(); i++) {
/*
!!We're changing the target below, i.e. we're changing the target implementation hence caching implementation will suffer!!
*/
				Target target = new Target(theTarget.getName(), 
																	 fragmentTargetURI(theTarget.getLocation(), String.valueOf(i)));
				target.setTarget(yamlRoots.get(i));
				target.setStage(Stage.parsed);
				targets.add(target);
			}
		}
	
		log.exiting(getClass().getName(), "parseTarget", theTarget);
    return targets;
  }

	protected URI fragmentTargetURI(URI theRoot, String theFragment) {
		try {
			return new URI(theRoot.getScheme(),
  	      					 theRoot.getSchemeSpecificPart(),
    	    					 theFragment);
		}
		catch(URISyntaxException urisx) {
			throw new RuntimeException();
		}
	}

  protected Target validateTarget(Target theTarget)
																						throws CheckerException {
		log.entering(getClass().getName(), "validateTarget", theTarget);
	
		String version = (String)
											((Map)theTarget.getTarget())
												.get("tosca_definitions_version");
		if (version == null)
			throw new CheckerException("Target " + theTarget + " does not specify a tosca_definitions_version");

		Target grammar = this.grammars.get(version);
		if (grammar == null)
			throw new CheckerException("Target " + theTarget + " specifies unknown tosca_definitions_version " + version);

		TOSCAValidator validator = null;
    try {
      validator = new TOSCAValidator(theTarget, grammar.getTarget());
    }
    catch (SchemaException sx) {
      throw new CheckerException("Grammar error at: " + sx.getPath(), sx);
    }

    theTarget.getReport().addAll(
				validator.validate(theTarget.getTarget()));
		theTarget.setStage(Stage.validated);
		
		if (!theTarget.getReport().hasErrors()) {
			//applyCanonicals(theTarget.getTarget(), validator.canonicals);
    }
    
		log.exiting(getClass().getName(), "validateTarget", theTarget);
		return theTarget;
  }

  /** */
  protected Target checkTarget(Target theTarget) throws CheckerException {
	
		log.entering(getClass().getName(), "checkTarget", theTarget);

		CheckContext ctx = new CheckContext(theTarget);
    //start at the top
		check_service_template_definition(
			(Map<String,Object>)theTarget.getTarget(), ctx);

		theTarget.setStage(Stage.checked);
		log.exiting(getClass().getName(), "checkTarget", theTarget);
		return theTarget;
	}

	private String errorReport(List<Throwable> theErrors) {
    StringBuilder sb = new StringBuilder(theErrors.size() + " errors");
    for (Throwable x: theErrors) {
      sb.append("\n");
			if (x instanceof ValidationException) {
        ValidationException vx = (ValidationException)x;
			  //.apend("at ")
        //.append(error.getLineNumber())
        //.append(" : ")
				sb.append("[")
          .append(vx.getPath())
          .append("] ");
      }
			else if (x instanceof TargetError) {
        TargetError tx = (TargetError)x;
				sb.append("[")
          .append(tx.getLocation())
          .append("] ");
			}
			sb.append(x.getMessage());
			if (x.getCause() != null) {
				sb.append("\n\tCaused by:\n")
          .append(x.getCause());
			}
    }
		sb.append("\n");
		return sb.toString();
	}


  protected void range_definition_post_validation_handler(Object theValue, Rule theRule, Validator.ValidationContext theContext) {
    log.entering("", "range_definition", theContext.getPath());

    assert theRule.getType().equals("seq");
    List bounds = (List)theValue;

    if (bounds.size() != 2) {
      theContext.addError("Too many values in bounds specification", theRule, theValue, null);
      return;
    }

    try { 
      Double.parseDouble(bounds.get(0).toString());  
    }  
    catch(NumberFormatException nfe) {
      theContext.addError("Lower bound not a number", theRule, theValue, null);
    }  

    try { 
      Double.parseDouble(bounds.get(1).toString());  
    }  
    catch(NumberFormatException nfe) {
      if (!"UNBOUNDED".equals(bounds.get(1).toString())) {
        theContext.addError("Upper bound not a number or 'UNBOUNDED'", theRule, theValue, null);
      }
    }  
    
  }
 
  public void check_properties(
								Map<String,Map> theDefinitions, CheckContext theContext) {
		theContext.enter("properties");
    try {
			if(!checkDefinition("properties", theDefinitions, theContext))
				return;

    	for (Iterator<Map.Entry<String,Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
      	Map.Entry<String,Map> e = i.next();
				check_property_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}

	protected void check_property_definition(
							String theName, Map theDefinition, CheckContext theContext) {
    theContext.enter(theName);
		if (!checkName(theName, theContext) ||
				!checkDefinition(theName, theDefinition, theContext)) {
			return;
    }
    //check the type
    if (!checkDataType (theName, theDefinition, theContext)) {
			return;
		}
    //check default value is compatible with type
		Object defaultValue = theDefinition.get("default");
		if (defaultValue != null) {
			checkDataValuation(defaultValue, theDefinition, theContext);
		}

		theContext.exit();
  }
  
	public void check_attributes(
								Map<String,Map> theDefinitions, CheckContext theContext) {
		theContext.enter("attributes");
    try {
			if(!checkDefinition("attributes", theDefinitions, theContext))
				return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_attribute_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}

  protected void check_attribute_definition(
								String theName, Map theDefinition, CheckContext theContext) {
    theContext.enter(theName);
		try {
			if (!checkName(theName, theContext) ||
					!checkDefinition(theName, theDefinition, theContext)) {
				return;
    	}
			if (!checkDataType(theName, theDefinition, theContext)) {
				return;
			}
		}
		finally {
			theContext.exit();
		}
  }
  
	/* top level rule, we collected the whole information set.
	 * this is where checking starts
	 */
  protected void check_service_template_definition(
										Map<String, Object> theDef, CheckContext theContext) {
    theContext.enter("");

		if (theDef == null) {
			theContext.addError("Empty template", null);
			return;
		}
		
		catalogs("", theDef, theContext); //root
//!!! imports need to be processed first now that catalogging takes place at check time!! 
		//first catalog whatever it is there to be cataloged so that the checks can perform cross-checking
		for (Iterator<Map.Entry<String,Object>> ri = theDef.entrySet().iterator();
		     ri.hasNext(); ) {
			Map.Entry<String,Object> e = ri.next();
			catalogs(e.getKey(), e.getValue(), theContext);
		}

		checks("", theDef, theContext); //root
		for (Iterator<Map.Entry<String,Object>> ri = theDef.entrySet().iterator();
		     ri.hasNext(); ) {
			Map.Entry<String,Object> e = ri.next();
			checks(e.getKey(), e.getValue(), theContext);
		}
    theContext.exit();
  }

	@Catalogs(path="/data_types")
  protected void catalog_data_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
		theContext.enter("data_types");
		try {
			catalogTypes(Construct.Data, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

	@Checks(path="/data_types")
  protected void check_data_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
		theContext.enter("data_types");
		
		try {
			if(!checkDefinition("data_types", theDefinitions, theContext))
		 		return;

    	for (Iterator<Map.Entry<String,Map>> i = theDefinitions.entrySet().iterator(); i.hasNext(); ) {
      	Map.Entry<String,Map> e = i.next();
				check_data_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

  /* */
  protected void check_data_type_definition(String theName,
	                                          Map theDefinition,
																						CheckContext theContext) {
    theContext.enter(theName, Construct.Data);
	  try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) { 
			  return;
      }

			checkTypeConstruct(
			  Construct.Data, theName, theDefinition, theContext);

      if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
		    checkTypeConstructFacet(Construct.Data, theName, theDefinition, 
																Facet.properties, theContext);
			}
    }
		finally {
		  theContext.exit();
		}
  }
	
	@Catalogs(path="/capability_types")
  protected void catalog_capability_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("capability_types");
		try {
			catalogTypes(Construct.Capability, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/capability_types")
  protected void check_capability_types(
	                    Map<String, Map> theTypes, CheckContext theContext) {
    theContext.enter("capability_types");
		try {
			if(!checkDefinition("capability_types", theTypes, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theTypes.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
      	check_capability_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

  /* */
  protected void check_capability_type_definition(String theName,
																									Map theDefinition,
																									CheckContext theContext) {
	  theContext.enter(theName, Construct.Capability);

    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Capability, theName, theDefinition, theContext);

      if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
      	checkTypeConstructFacet(Construct.Capability, theName, theDefinition, 
																Facet.properties, theContext);
      }

      if (theDefinition.containsKey("attributes")) {
      	check_attributes(
					(Map<String,Map>)theDefinition.get("attributes"), theContext);
	    	checkTypeConstructFacet(Construct.Capability, theName, theDefinition,
																Facet.attributes, theContext);
			}

	    //valid_source_types: see capability_type_definition
  		//unclear: how is the valid_source_types list definition eveolving across
	  	//the type hierarchy: additive, overwriting, ??
		  if (theDefinition.containsKey("valid_source_types")) {
			  checkTypeReference(Construct.Node, theContext, 
				  ((List<String>)theDefinition.get("valid_source_types")).toArray(EMPTY_STRING_ARRAY));
		  }
    }
		finally {
		  theContext.exit();
		}
	}

	@Catalogs(path="/relationship_types")
  protected void catalog_relationship_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
		theContext.enter("relationship_types");
		try {
			catalogTypes(Construct.Relationship, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/relationship_types")
  protected void check_relationship_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
		theContext.enter("relationship_types");
		try {
			if(!checkDefinition("relationship_types", theDefinition, theContext))
		 		return;

    	for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
      	Map.Entry<String,Map> e = i.next();
      	check_relationship_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }
 
  /* */
	protected void check_relationship_type_definition(String theName,
																										Map theDefinition,
																										CheckContext theContext) {
    theContext.enter(theName, Construct.Relationship);
    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Relationship, theName, theDefinition, theContext);
		  
      if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
				checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
											 					Facet.properties, theContext); 
			}
			
      if (theDefinition.containsKey("attributes")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("attributes"), theContext);
				checkTypeConstructFacet(Construct.Relationship, theName, theDefinition,
											 					Facet.attributes, theContext);
			}

			Map<String,Map> interfaces = (Map<String,Map>)theDefinition.get("interfaces");
      if (interfaces != null) {
			  theContext.enter("interfaces");
        for (Iterator<Map.Entry<String,Map>> i =
								interfaces.entrySet().iterator(); i.hasNext(); ) {
          Map.Entry<String,Map> e = i.next();
          check_type_interface_definition(
						e.getKey(), e.getValue(), theContext);
        }
				theContext.exit();
      }
      
		  if (theDefinition.containsKey("valid_target_types")) {
			  checkTypeReference(Construct.Capability, theContext, 
				  ((List<String>)theDefinition.get("valid_target_types")).toArray(EMPTY_STRING_ARRAY));
			}
    }
		finally {
		  theContext.exit();
		}
  }

	@Catalogs(path="/artifact_types")
  protected void catalog_artifact_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("artifact_types");
		try {
			catalogTypes(Construct.Artifact, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/artifact_types")
	protected void check_artifact_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
    theContext.enter("artifact_types");
		try {
			if(!checkDefinition("artifact_types", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_artifact_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
    	theContext.exit();
		}
  }
	
	/* */
	protected void check_artifact_type_definition(String theName,
																								Map theDefinition,
																								CheckContext theContext) {
    theContext.enter(theName, Construct.Artifact);
    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Artifact, theName, theDefinition, theContext);
		}
		finally {
		  theContext.exit();
		}
  }

	@Catalogs(path="/interface_types")
  protected void catalog_interface_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("interface_types");
		try {
			catalogTypes(Construct.Interface, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

	/* */
	@Checks(path="/interface_types")
  protected void check_interface_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
    theContext.enter("interface_types");
		try {
			if(!checkDefinition("interface_types", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_interface_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }
	
	/* */
	protected void check_interface_type_definition(String theName,
																								 Map theDefinition,
																								 CheckContext theContext) {
    theContext.enter(theName, Construct.Interface);
    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Interface, theName, theDefinition, theContext);

			//not much else here: a list of operation_definitions, each with its
			//implementation and inputs

			//check that common inputs are re-defined in a compatible manner

			//check that the interface operations are overwritten in a compatible manner
			//for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet()

    }
		finally {
		  theContext.exit();
		}
  }

	@Catalogs(path="/node_types")
  protected void catalog_node_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("node_types");
		try {
			catalogTypes(Construct.Node, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/node_types")
  protected void check_node_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
    theContext.enter("node_types");
		try {
			if(!checkDefinition("node_types", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_node_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

	
	/* */
	protected void check_node_type_definition(String theName,
																						Map theDefinition,
																						CheckContext theContext) {
    theContext.enter(theName, Construct.Node);
      
	  try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Node, theName, theDefinition, theContext);
		  
      if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
				checkTypeConstructFacet(Construct.Node, theName, theDefinition,
																Facet.properties, theContext);
      }

      if (theDefinition.containsKey("attributes")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("attributes"), theContext);
				checkTypeConstructFacet(Construct.Node, theName, theDefinition, 
																Facet.attributes, theContext);
			}

  		//requirements
      if (theDefinition.containsKey("requirements")) {
				check_requirements(
				  (List<Map>)theDefinition.get("requirements"), theContext);
			}

  	  //capabilities
      if (theDefinition.containsKey("capabilities")) {
				check_capabilities(
				  (Map<String,Map>)theDefinition.get("capabilities"), theContext);
			}
	  
		  //interfaces: 
			Map<String,Map> interfaces = 
													(Map<String,Map>)theDefinition.get("interfaces");
      if (interfaces != null) {
				try {
			  	theContext.enter("interfaces");
        	for (Iterator<Map.Entry<String,Map>> i =
									interfaces.entrySet().iterator(); i.hasNext(); ) {
          	Map.Entry<String,Map> e = i.next();
          	check_type_interface_definition(
							e.getKey(), e.getValue(), theContext);
        	}
				}
				finally {
					theContext.exit();
				}
      }
		  
			//artifacts
      
    }
		finally {
		  theContext.exit();
		}
  }
  
	@Catalogs(path="/group_types")
  protected void catalog_group_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("group_types");
		try {
			catalogTypes(Construct.Group, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

	/* */
	@Checks(path="/group_types")
  protected void check_group_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
    theContext.enter("group_types");
		try {
			if(!checkDefinition("group_types", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_group_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }
	
	/* */
	protected void check_group_type_definition(String theName,
																						Map theDefinition,
																						CheckContext theContext) {
    theContext.enter(theName, Construct.Group);
      
	  try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) {
			  return;
			}

		  checkTypeConstruct(
			  Construct.Group, theName, theDefinition, theContext);
      
			if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
				checkTypeConstructFacet(Construct.Group, theName, theDefinition,
																Facet.properties, theContext);
      }

		  if (theDefinition.containsKey("targets")) {
			  checkTypeReference(Construct.Node, theContext, 
				  ((List<String>)theDefinition.get("targets")).toArray(EMPTY_STRING_ARRAY));
		  }

			//interfaces
			Map<String,Map> interfaces = 
													(Map<String,Map>)theDefinition.get("interfaces");
      if (interfaces != null) {
				try {
				  theContext.enter("interfaces");
      	  for (Iterator<Map.Entry<String,Map>> i =
									interfaces.entrySet().iterator(); i.hasNext(); ) {
     	     Map.Entry<String,Map> e = i.next();
      	    check_type_interface_definition(
							e.getKey(), e.getValue(), theContext);
        	}
				}
				finally {
					theContext.exit();
				}
      }

		}
		finally {
			theContext.exit();
		}
	}
	
	@Catalogs(path="/policy_types")
  protected void catalog_policy_types(
										Map<String, Map> theDefinitions, CheckContext theContext) {
    theContext.enter("policy_types");
		try {
			catalogTypes(Construct.Policy, theDefinitions, theContext);
		}
		finally {
			theContext.exit();
		}
	}

	/* */
	@Checks(path="/policy_types")
  protected void check_policy_types(
										Map<String, Map> theDefinition, CheckContext theContext) {
    theContext.enter("policy_types");
		try {
			if(!checkDefinition("policy_types", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_policy_type_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

	/* */
	protected void check_policy_type_definition(String theName,
																						Map theDefinition,
																						CheckContext theContext) {
    theContext.enter(theName, Construct.Policy);
      
	  try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDefinition, theContext)) { 
			  return;
			}
		  
			checkTypeConstruct(
			  Construct.Policy, theName, theDefinition, theContext);

			if (theDefinition.containsKey("properties")) {
      	check_properties(
					(Map<String,Map>)theDefinition.get("properties"), theContext);
				checkTypeConstructFacet(Construct.Policy, theName, theDefinition,
																Facet.properties, theContext);
      }

			//the targets can be known node types or group types
			List<String> targets = (List<String>)theDefinition.get("targets");
			if (targets != null) {
			  if (checkDefinition("targets", targets, theContext)) {
					for (String target: targets) {
						if (!(this.catalog.hasType(Construct.Node, target) ||
								  this.catalog.hasType(Construct.Group, target))) {
							theContext.addError(
								Message.INVALID_TYPE_REFERENCE, "targets", target, Arrays.asList(Construct.Node, Construct.Group));
						}
					}
				}
		  }

		}
		finally {
			theContext.exit();
		}
	}

  //checking of actual constructs (capability, ..)

	/* First, interface types do not have a hierarchical organization (no 
	 * 'derived_from' in a interface type definition).
	 * So, when interfaces (with a certain type) are defined in a node
	 * or relationship type (and they can define new? operations), what
	 * is there to check:
	 * 	Can operations here re-define their declaration from the interface
	 * type spec?? From A.5.11.3 we are to understand indicates override to be
	 * the default interpretation .. but they talk about sub-classing so it
	 * probably intended as a reference to the node or relationship type
	 * hierarchy and not the interface type (no hierarchy there).
	 *	Or is this a a case of augmentation where new operations can be added??
	 */
  protected void check_type_interface_definition(
			String theName, Map theDef, CheckContext theContext) {
    theContext.enter(theName);
    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}

			if (!checkTypeReference(Construct.Interface, theContext, (String)theDef.get("type")))
				return;

      if (theDef.containsKey("inputs")) {
				check_inputs((Map<String, Map>)theDef.get("inputs"), theContext);
			}

			//operations: all entries except for 'type' and 'inputs'
		/*
      for (Iterator<Map.Entry<String,Map>> i = 
							theDef.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry<String,Map> e = i.next();
				String ename = e.getKey();
				if ("type".equals(ename) || "inputs".equals(ename)) {
					continue;
				}
			  ?? check_operation_definition(ename, e.getValue(), theContext);
		  }
		*/
		}
    finally {
		  theContext.exit();
		}
  } 

  /* */
	protected void check_capabilities(Map<String,Map> theDefinition,
																		CheckContext theContext) {
    theContext.enter("capabilities");
		try {
			if(!checkDefinition("capabilities", theDefinition, theContext))
		 		return;

	    for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
  	    Map.Entry<String,Map> e = i.next();
				check_capability_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}

  /* A capability definition appears within the context ot a node type
	 */
  protected void check_capability_definition(String theName,
	                                           Map theDef,
	                                           CheckContext theContext) {
    theContext.enter(theName, Construct.Capability);

    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}

      //check capability type
		  if(!checkTypeReference(Construct.Capability, theContext, (String)theDef.get("type")))
        return;
    
      //check properties
      if (!checkFacetAugmentation(
			  		Construct.Capability, theDef, Facet.properties, theContext))
        return;    

      //check attributes
      if (!checkFacetAugmentation(
				  	Construct.Capability, theDef, Facet.attributes, theContext))
        return;    

      //valid_source_types: should point to valid template nodes
		  if (theDef.containsKey("valid_source_types")) {
			  checkTypeReference(Construct.Node, theContext, 
				  ((List<String>)theDef.get("valid_source_types")).toArray(EMPTY_STRING_ARRAY));
				//per A.6.1.4 there is an additinal check to be performed here: 
				//"Any Node Type (names) provides as values for the valid_source_types keyname SHALL be type-compatible (i.e., derived from the same parent Node Type) with any Node Types defined using the same keyname in the parent Capability Type."
			}
      //occurences: were verified in range_definition

		}
    finally {
		  theContext.exit();
		}
  } 

  protected void check_artifact_definition(String theName,
																					 Map theDef,
																					 CheckContext theContext) {
    theContext.enter(theName, Construct.Artifact);

    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}
      //check artifact type
		  if(!checkTypeReference(Construct.Artifact, theContext, (String)theDef.get("type"))) 
        return;
		}
		finally {
		  theContext.exit();
		}
  }

	protected void check_requirements(List<Map> theDefinition,
																		CheckContext theContext) {
    theContext.enter("requirements");
    try {
			if(!checkDefinition("requirements", theDefinition, theContext))
		 		return;
    
			for (Iterator<Map> i = theDefinition.iterator(); i.hasNext(); ) {
      	Map e = i.next();
				Iterator<Map.Entry<String, Map>> ei =
										(Iterator<Map.Entry<String, Map>>)e.entrySet().iterator();
				Map.Entry<String, Map> eie = ei.next();
				check_requirement_definition(eie.getKey(), eie.getValue(), theContext);
				assert ei.hasNext() == false;
    	}
		}
		finally {
			theContext.exit();
		}
	}

  protected void check_requirement_definition(String theName,
																							Map theDef,
																							CheckContext theContext) {
    theContext.enter(theName, Construct.Requirement);

    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}
      //check capability type
      String capabilityType = (String)theDef.get("capability");
      if (null != capabilityType) {
	   		checkTypeReference(Construct.Capability, theContext, capabilityType); 
      }

      //check node type
      String nodeType = (String)theDef.get("node");
      if (null != nodeType) {
		  	checkTypeReference(Construct.Node, theContext, nodeType); 
      }

      //check relationship type
      Map relationshipSpec = (Map)theDef.get("relationship");
			String relationshipType = null;
      if (null != relationshipSpec) {
        relationshipType = (String)relationshipSpec.get("type");
		  	if (relationshipType != null) { //should always be the case
  		  	checkTypeReference(Construct.Relationship,theContext,relationshipType); 
			  }

			  Map<String,Map> interfaces = (Map<String,Map>)
																					relationshipSpec.get("interfaces");
        if (interfaces != null) {
				  //augmentation (additional properties or operations) of the interfaces
				  //defined by the above relationship types

					//check that the interface types are known
					for (Map interfaceDef : interfaces.values()) {
						checkTypeReference(Construct.Interface, theContext, (String)interfaceDef.get("type"));
					}
			  }
      }

			//cross checks

			//the capability definition might come from the capability type or from the capability definition
			//within the node type. We might have more than one as a node might specify multiple capabilities of the
			//same type.
			//the goal here is to cross check the compatibility of the valid_source_types specification in the 
			//target capability definition (if that definition contains a valid_source_types entry). 
			List<Map> capabilityDefs = new LinkedList<Map>();
			//nodeType exposes capabilityType
			if (nodeType != null) {
				Map<String,Map> capabilities =
								findTypeFacetByType(Construct.Node, nodeType,
																		Facet.capabilities, capabilityType);
				if (capabilities.isEmpty()) {
					theContext.addError("The node type " + nodeType + " does not appear to expose a capability of a type compatible with " + capabilityType, null);
				}
				else {
					for (Map.Entry<String,Map> capability: capabilities.entrySet()) {
						//this is the capability as it was defined in the node type
						Map capabilityDef = capability.getValue();
						//if it defines a valid_source_types then we're working with it,
						//otherwise we're working with the capability type it points to.
						//The spec does not make it clear if the valid_source_types in a capability definition augments or
						//overwrites the one from the capabilityType (it just says they must be compatible).
						if (capabilityDef.containsKey("valid_source_types")) {
							capabilityDefs.add(capabilityDef);
						}
						else {
							capabilityDef =
								catalog.getTypeDefinition(Construct.Capability, (String)capabilityDef.get("type"));
							if (capabilityDef.containsKey("valid_source_types")) {
								capabilityDefs.add(capabilityDef);
							}
							else {
								//!!if there is a capability that does not have a valid_source_type than there is no reason to
								//make any further verification (as there is a valid node_type/capability target for this requirement)
								capabilityDefs.clear();
								break;
							}
						}
					}
				}
			}
			else {
				Map capabilityDef =	catalog.getTypeDefinition(Construct.Capability, capabilityType);
				if (capabilityDef.containsKey("valid_source_types")) {
					capabilityDefs.add(capabilityDef);
				}
			}

			//check that the node type enclosing this requirement definition
			//is in the list of valid_source_types
			if (!capabilityDefs.isEmpty()) {
				String enclosingNodeType =
										theContext.enclosingConstruct(Construct.Node).name();
				assert enclosingNodeType != null;

				if (!capabilityDefs.stream().anyMatch(
							(Map capabilityDef)->{
								List<String> valid_source_types =
												(List<String>)capabilityDef.get("valid_source_types");
								return valid_source_types.stream().anyMatch(
									(String source_type)-> catalog.isDerivedFrom(
												Construct.Node, enclosingNodeType, source_type));
							})) {
						theContext.addError("Node type: " + enclosingNodeType + " not compatible with any of the valid_source_types provided in the definition of compatible capabilities", null);

				}

				/*
				boolean found = false;
				for (Map capabilityDef: capabilityDefs) {

					List<String> valid_source_types =
											(List<String>)capabilityDef.get("valid_source_types");
					String enclosingNodeType =
										theContext.enclosingConstruct(Construct.Node);
					assert enclosingNodeType != null;

					//make sure enclosingNodeType is compatible (same or derived from)
					//one valid source type
					for (String source_type: valid_source_types) {
						if (catalog.isDerivedFrom(
									Construct.Node, enclosingNodeType, source_type)) {
							found = true;
							break;
						}
					}
				}
				
				if (!found) {
						//the message is not great as it points to the declared
						//capabilityType which is not necessarly where the information
						//is coming from
						theContext.addError("Node type: " + enclosingNodeType + " not compatible with any of the valid_source_types " + valid_source_types + " provided in the definition of capability " + capabilityType, null);
				}
				*/
			}

			//if we have a relationship type, check if it has a valid_target_types
			//if it does, make sure that the capability type is compatible with one
			//of them
		 	if (relationshipType != null) { //should always be the case
				Map relationshipTypeDef = catalog.getTypeDefinition(
																		Construct.Relationship, relationshipType);
				if (relationshipTypeDef != null) {
					List<String> valid_target_types =
								(List<String>)relationshipTypeDef.get("valid_target_types");
					if (valid_target_types != null) {
						boolean found = false;
						for (String target_type: valid_target_types) {
							if (catalog.isDerivedFrom(
										Construct.Capability, capabilityType, target_type)) {
								found = true;
								break;
							}
						}
						if (!found) {
							theContext.addError("Capability type: " + capabilityType + " not compatible with any of the valid_target_types " + valid_target_types + " provided in the definition of relationship type " + relationshipType, null);
						}
					}
				}
			}

			//relationship declares the capabilityType in its valid_target_type set
      //in A.6.9 'Relationship Type' the spec does not indicate how	inheritance
			//is to be applied to the valid_target_type spec: cumulative, overwrites, 
			//so we treat it as an overwrite.
		}
		finally {
		  theContext.exit();
		}
  }
  
	//topology_template_definition and sub-rules
	/* */
	@Catalogs(path="/topology_template")
	protected void catalog_topology_template(
										Map<String,Map> theDef, final CheckContext theContext) {
    
		theContext.enter("topology_template");

		try {		
			theDef.entrySet().stream()
				.forEach(e ->	catalogs(e.getKey(), e.getValue(), theContext));
		}
		finally {
			theContext.exit();
		}
  }

	/** */
	@Checks(path="/topology_template")
	protected void check_topology_template(
										Map<String,Map> theDef, final CheckContext theContext) {
    
		theContext.enter("topology_template");

		try {		
		//	theDef.entrySet().stream()
		//		.forEach(e ->	catalogs(e.getKey(), e.getValue(), theContext));
		
			theDef.entrySet().stream()
				.forEach(e ->	checks(e.getKey(), e.getValue(), theContext));
		}
		finally {
			theContext.exit();
		}
  }


  /*
   * Once the syntax of the imports section is validated parse/validate/catalog    * all the imported template information
   */
	@Checks(path="/imports")
  protected void check_imports(List theImports, CheckContext theContext) {
    theContext.enter("imports");

		for (ListIterator li = theImports.listIterator(); li.hasNext(); ) {
			Object importEntry = li.next(),
						 importFile =  ((Map)mapEntry(importEntry).getValue()).get("file");
			Target tgt = null;
			try {
				tgt = catalog.getTarget( (URI)importFile );
			}
			catch (ClassCastException ccx) {
				System.out.println("Import is " + importFile);
			}

			if (tgt == null) {
				//malfunction
    	  theContext.addError("Checking import '" + importFile + "': failed at a previous stage", null);
				return;
			}

			if (tgt.getReport().hasErrors()) {
				//import failed parsing or validation, we skip it
				continue;
			}

			if (tgt.getStage() == Stage.checked) {
				//been here before, this target had already been processed
				continue;
			}

			//import should have been fully processed by now ??? 
			log.log(Level.FINE, "Processing import " + tgt + ".");
   	  try {
      	checkTarget(tgt);
     	}
   	  catch (CheckerException cx) {
    	  theContext.addError("Failure checking import '" + tgt + "'", cx);
      }
		 
    }
		theContext.exit();
  }

	/* */
	@Checks(path="/topology_template/substitution_mappings")
  protected void check_substitution_mappings(Map<String, Object> theSub,
	                            							 CheckContext theContext) {
    theContext.enter("substitution_mappings");
		try {
			//type is mandatory
			String type = (String)theSub.get("node_type");
  		if (!checkTypeReference(Construct.Node, theContext, type)) {
				return;
			}

			Map<String,List> capabilities = (Map<String,List>)theSub.get("capabilities");
			if (null != capabilities) {
				for (Map.Entry<String,List> ce: capabilities.entrySet()) {
					//the key must be a capability of the type
					if (null == findTypeFacetByName(Construct.Node, type,
																					Facet.capabilities, ce.getKey())) {
    				theContext.addError("Unknown node type capability: " + ce.getKey() + ", type " + type, null);
					}
					//the value is a 2 element list: first is a local node, 
					//second is the name of one of its capabilities
					List target = ce.getValue();
					if (target.size() != 2) {
    				theContext.addError("Invalid capability mapping: " + target + ", expecting 2 elements", null);
						continue;
					}

					String targetNode = (String)target.get(0),
								 targetCapability = (String)target.get(1);
				
					Map<String,Object> targetNodeDef = (Map<String,Object>)
												this.catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
					if (null == targetNodeDef) {
    				theContext.addError("Invalid capability mapping node template: " + targetNode, null);
						continue;
					}

					String targetNodeType = (String)targetNodeDef.get("type");
					if (null == findTypeFacetByName(Construct.Node, targetNodeType,
																		Facet.capabilities, targetCapability)) {
    				theContext.addError("Invalid capability mapping capability: " + targetCapability + ". No such capability found for node template " + targetNode + ", of type " + targetNodeType, null);
					}
				}
			}

			Map<String,List> requirements = (Map<String,List>)theSub.get("requirements");
			if (null != requirements) {
				for (Map.Entry<String,List> re: requirements.entrySet()) {
					//the key must be a requirement of the type
					if (null == findNodeTypeRequirementByName(type, re.getKey())) {
    				theContext.addError("Unknown node type requirement: " + re.getKey() + ", type " + type, null);
					}

					List target = re.getValue();
					if (target.size() != 2) {
    				theContext.addError("Invalid requirement mapping: " + target + ", expecting 2 elements", null);
						continue;
					}

					String targetNode = (String)target.get(0),
								 targetRequirement = (String)target.get(1);
				
					Map<String,Object> targetNodeDef = (Map<String,Object>)
												this.catalog.getTemplate(theContext.target(), Construct.Node, targetNode);
					if (null == targetNodeDef) {
    				theContext.addError("Invalid requirement mapping node template: " + targetNode, null);
						continue;
					}

					String targetNodeType = (String)targetNodeDef.get("type");
					if (null == findNodeTypeRequirementByName(targetNodeType,targetRequirement)) {
    				theContext.addError("Invalid requirement mapping requirement: " + targetRequirement + ". No such requirement found for node template " + targetNode + ", of type " + targetNodeType, null);
					}
				}
			}
		}
		finally {
		  theContext.exit();
		}
	}

  /* */
	@Catalogs(path="/topology_template/inputs")
  protected void catalog_inputs(Map<String, Map> theInputs,
	                            	CheckContext theContext) {
    theContext.enter("inputs");

		try {
			catalogTemplates(Construct.Data, theInputs, theContext);
    }
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/topology_template/inputs")
  protected void check_inputs(Map<String, Map> theInputs,
	                            CheckContext theContext) {
    theContext.enter("inputs");

    try {
			if(!checkDefinition("inputs", theInputs, theContext))
		 		return;
    
		  for (Iterator<Map.Entry<String,Map>> i = theInputs.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String,Map> e = i.next();
				check_input_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

	/* */
  protected void check_input_definition(String theName,
																				Map theDef,
																				CheckContext theContext) {
    theContext.enter(theName);
		try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}
			//
      if (!checkDataType(theName, theDef, theContext)) {
				return;
			}
      //check default value
			Object defaultValue = theDef.get("default");
			if (defaultValue != null) {
				checkDataValuation(defaultValue, theDef, theContext);
			}
		}
		finally {
		  theContext.exit();
		}
	}
  
	@Checks(path="/topology_template/outputs")
	protected void check_outputs(Map<String, Map> theOutputs,
	                            CheckContext theContext) {
    theContext.enter("outputs");

    try {
			if(!checkDefinition("outputs", theOutputs, theContext))
		 		return;
    
		  for (Iterator<Map.Entry<String,Map>> i = theOutputs.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String,Map> e = i.next();
				check_output_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }
  
	protected void check_output_definition(String theName,
																				Map theDef,
																				CheckContext theContext) {
    theContext.enter(theName);
		try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}
      
      //check value
			Object value = theDef.get("value");
			if (value != null) {
				checkDataValuation(value, theDef, theContext);
			}
		}
		finally {
		  theContext.exit();
		}
	}
	
	@Checks(path="/topology_template/groups")
	protected void check_groups(Map<String, Map> theGroups,
	                            CheckContext theContext) {
    theContext.enter("groups");

    try {
			if(!checkDefinition("groups", theGroups, theContext))
		 		return;
    
		  for (Iterator<Map.Entry<String,Map>> i = theGroups.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<String,Map> e = i.next();
				check_group_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

	protected void check_group_definition(String theName,
																				Map theDef,
																				CheckContext theContext) {
    theContext.enter(theName);
		try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}

      if (!checkTypeReference(Construct.Group, theContext, (String)theDef.get("type")))
        return;
			
			if (!checkFacet(
					   Construct.Group, theDef, Facet.properties, theContext))
        return;    
		  
			if (theDef.containsKey("targets")) {
			  //checkTemplateReference(Construct.Node, theContext, 
				//  ((List<String>)theDef.get("targets")).toArray(EMPTY_STRING_ARRAY));
				
				List<String> targetsTypes = (List<String>)
					this.catalog.getTypeDefinition(Construct.Group,
																				 (String)theDef.get("type"))
												.get("targets");

				List<String> targets = (List<String>)theDef.get("targets");
				for (String target: targets) {
					if (!this.catalog.hasTemplate(theContext.target(),Construct.Node, target)) {
						theContext.addError("The 'targets' entry must contain a reference to a node template, '" + target + "' is not one", null);
					}
					else {
						if (targetsTypes != null) {
							String targetType = (String)
								this.catalog.getTemplate(theContext.target(), Construct.Node, target).get("type");
						
							boolean found = false;
							for (String type: targetsTypes) {
								found = this.catalog
												.isDerivedFrom(Construct.Node, targetType, type);
								if (found)
									break;
							}
						
							if (!found) {
								theContext.addError("The 'targets' entry '" + target + "' is not type compatible with any of types specified in policy type targets", null);
							}
						}
					}
				}
		  }
			
			if (theDef.containsKey("interfaces")) {
			}
		}
		finally {
		  theContext.exit();
		}
	}

	@Checks(path="/topology_template/policies")
	protected void check_policies(List<Map<String,Map>> thePolicies,
	                              CheckContext theContext) {
    theContext.enter("policies");

    try {
			if(!checkDefinition("policies", thePolicies, theContext))
		 		return;
   
		 	for (Map<String,Map> policy: thePolicies) {
				assert policy.size() == 1;	
				Map.Entry<String,Map> e = policy.entrySet().iterator().next();
				check_policy_definition(e.getKey(), e.getValue(), theContext);
			}
		}
		finally {
			theContext.exit();
		}
  }
	
	protected void check_policy_definition(String theName,
																				 Map theDef,
																				 CheckContext theContext) {
    theContext.enter(theName);
		try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theDef, theContext)) {
			  return;
			}
      
			if (!checkTypeReference(Construct.Policy, theContext, (String)theDef.get("type")))
        return;
      
			if (!checkFacet(
					   Construct.Policy, theDef, Facet.properties, theContext))
        return;

			//targets: must point to node or group templates (that are of a type
			//specified in the policy type definition, if targets were specified
			//there).
		  if (theDef.containsKey("targets")) {
				List<String> targetsTypes = (List<String>)
					this.catalog.getTypeDefinition(Construct.Policy,
																				 (String)theDef.get("type"))
												.get("targets");

				List<String> targets = (List<String>)theDef.get("targets");
				for (String target: targets) {
					Construct targetConstruct = null;

					if (this.catalog.hasTemplate(theContext.target(),Construct.Group, target)) {
						targetConstruct = Construct.Group;
					}
					else if (this.catalog.hasTemplate(theContext.target(),Construct.Node, target)) {
						targetConstruct = Construct.Node;
					}
					else {
						theContext.addError(Message.INVALID_TEMPLATE_REFERENCE, "targets", target, new Object[] {"node", "group"});
					}
						
					if (targetConstruct != null &&
							targetsTypes != null) {
						//get the target type and make sure is compatible with the types
						//indicated in the type spec
						String targetType = (String)
								this.catalog.getTemplate(theContext.target(), targetConstruct, target).get("type");	

						boolean found = false;
						for (String type: targetsTypes) {
							found = this.catalog
												.isDerivedFrom(targetConstruct, targetType, type);
							if (found)
								break;
						}
						
						if (!found) {
							theContext.addError("The 'targets' " + targetConstruct + " entry '" + target + "' is not type compatible with any of types specified in policy type targets", null);
						}
					}
				}
		  }

			if (theDef.containsKey("triggers")) {
				List<Map> triggers = (List<Map>)theDef.get("triggers");
				//TODO
			}

		}
		finally {
		  theContext.exit();
		}
	}
  
	/* */
	@Catalogs(path="/topology_template/node_templates")
  protected void catalog_node_templates(Map<String, Map> theTemplates,
	                            					CheckContext theContext) {
    theContext.enter("node_templates");

		try {
			catalogTemplates(Construct.Node, theTemplates, theContext);
    }
		finally {
			theContext.exit();
		}
	}

  /* */
	@Checks(path="/topology_template/node_templates")
  protected void check_node_templates(Map<String, Map> theTemplates,
	                                    CheckContext theContext) {
    theContext.enter("node_templates");
    try {
			if(!checkDefinition("node_templates", theTemplates, theContext))
		 		return;

	    for (Iterator<Map.Entry<String,Map>> i = theTemplates.entrySet().iterator(); i.hasNext(); ) {
  	    Map.Entry<String,Map> e = i.next();
				check_node_template_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
  }

	/* */
  protected void check_node_template_definition(String theName,
	                                              Map theNode,
                                                CheckContext theContext) {
    theContext.enter(theName, Construct.Node);

    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theNode, theContext)) {
			  return;
			}

      if (!checkTypeReference(Construct.Node, theContext, (String)theNode.get("type")))
        return;
			
			//copy
			String copy = (String)theNode.get("copy");
			if (copy != null) {
				if (!checkTemplateReference(Construct.Node, theContext, copy)) {
					theContext.addError(Message.INVALID_TEMPLATE_REFERENCE, "copy", copy, Construct.Node);
				}
				else {
					//the 'copy' node specification should be used to provide 'defaults'
					//for this specification, we should check them
				}
			}

      /* check that we operate on properties and attributes within the scope of
	  	the specified node type */
      if (!checkFacet(
					   Construct.Node, /*theName,*/theNode, Facet.properties, theContext))
        return;    

      if (!checkFacet(
						 Construct.Node, /*theName,*/theNode, Facet.attributes, theContext))
        return;    

    	//requirement assignment seq
			if (theNode.containsKey("requirements")) {
				check_requirements_assignment_definition(
					(List<Map>)theNode.get("requirements"), theContext);
			}

			//capability assignment map: subject to augmentation
			if (theNode.containsKey("capabilities")) {
				check_capabilities_assignment_definition(
						(Map<String,Map>)theNode.get("capabilities"), theContext);
			}

			//interfaces
			if (theNode.containsKey("interfaces")) {
				check_template_interfaces_definition(
						(Map<String,Map>)theNode.get("interfaces"), theContext);
			}

			//artifacts: artifacts do not have different definition forms/syntax 
			//depending on the context (type or template) but they are still subject 
			//to 'augmentation'
			if (theNode.containsKey("artifacts")) {
				check_template_artifacts_definition(
						(Map<String,Object>)theNode.get("artifacts"), theContext);
			}

			/* node_filter: the context to which the node filter is applied is very
			 * wide here as opposed to the node filter specification in a requirement
			 * assignment which has a more strict context (target node/capability are
			 * specified).
			 * We could check that there are nodes in this template having the 
			 * properties/capabilities specified in this filter, i.e. the filter has
			 * a chance to succeed.
			 */
    }
		finally {
			theContext.exit();
		}
  }
  
	@Checks(path="/topology_template/relationship_templates")
  protected void check_relationship_templates(Map theTemplates,
	                                            CheckContext theContext) {
    theContext.enter("relationship_templates");

    for (Iterator<Map.Entry<String,Map>> i = theTemplates.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry<String,Map> e = i.next();
			check_relationship_template_definition(e.getKey(), e.getValue(), theContext);
    }
		theContext.exit();
  }

  /* */
  protected void check_relationship_template_definition(
																								String theName,
																								Map theRelationship,
																								CheckContext theContext) {
    theContext.enter(theName, Construct.Relationship);
    try {
		  if (!checkName(theName, theContext) ||
			    !checkDefinition(theName, theRelationship, theContext)) {
			  return;
			}

      if (!checkTypeReference(Construct.Relationship, theContext, (String)theRelationship.get("type")))
        return;
			
			String copy = (String)theRelationship.get("copy");
			if (copy != null) {
				if (!checkTemplateReference(Construct.Relationship, theContext, copy)) {
					theContext.addError(Message.INVALID_TEMPLATE_REFERENCE, "copy", copy, Construct.Relationship);
				}
			}

      /* check that we operate on properties and attributes within the scope of
	  	the specified relationship type */
      if (!checkFacet(Construct.Relationship, theRelationship,
											Facet.properties, theContext))
        return;    

      if (!checkFacet(Construct.Relationship, theRelationship,
											Facet.attributes, theContext))
      return;    

    /* interface definitions
		   note: augmentation is allowed here so not clear what to check ..
			 maybe report augmentations if so configured .. */

    }
		finally {
			theContext.exit();
		}
	}

  //requirements and capabilities assignment appear in a node templates
  protected void check_requirements_assignment_definition(
										List<Map> theRequirements, CheckContext theContext) {
		theContext.enter("requirements");
    try {
			if(!checkDefinition("requirements", theRequirements, theContext))
		 		return;
		
			//the node type for the node template enclosing these requirements
			String nodeType =	(String)catalog.getTemplate(
															theContext.target(),
															Construct.Node,
															theContext.enclosingConstruct(Construct.Node).name())
																.get("type");
		
			for(Iterator<Map> ri = theRequirements.iterator(); ri.hasNext(); ) {
				Map<String,Map> requirement = (Map<String,Map>)ri.next();
			
				Iterator<Map.Entry<String,Map>> rai = 
					(Iterator<Map.Entry<String,Map>>)requirement.entrySet().iterator();
			
				Map.Entry<String,Map> requirementEntry = rai.next();
				assert !rai.hasNext();

				String requirementName = requirementEntry.getKey();
				Map		 requirementDef = findNodeTypeRequirementByName(
																								nodeType, requirementName);

				if (requirementDef == null /*&&
						!config.allowAugmentation()*/) {
					theContext.addError("No requirement " + requirementName + " was defined for the node type " + nodeType, null);
					continue;
				}

				check_requirement_assignment_definition(
					requirementName, requirementEntry.getValue(), requirementDef, theContext);
			}
		}
		finally {
    	theContext.exit();
		}
	}
	
	protected void check_requirement_assignment_definition(
																			String theRequirementName,
																			Map theAssignment,
																			Map theDefinition,
																			CheckContext theContext) {
		theContext//.enter("requirement_assignment")
							.enter(theRequirementName, Construct.Requirement);
		
		//grab the node type definition to verify compatibility

	  try {
			//node assignment
			boolean targetNodeIsTemplate = false;
			String targetNode = (String)theAssignment.get("node");
			if (targetNode == null) {
				targetNode = (String)theDefinition.get("node");
				//targetNodeIsTemplate stays false, targetNode must be a type
			}
			else {
				//the value must be a node template or a node type
				targetNodeIsTemplate = isTemplateReference(
																	Construct.Node, theContext, targetNode);
				if (!targetNodeIsTemplate) {
			  	if (!isTypeReference(Construct.Node/*, theContext*/, targetNode)) {
						theContext.addError(Message.INVALID_CONSTRUCT_REFERENCE, "node", targetNode, Construct.Node);
						return;
					}
					//targetNode is a type reference
				}
			
				//additional checks
				String targetNodeDef = (String)theDefinition.get("node");
				if (targetNodeDef != null && targetNode != null) {
					if (targetNodeIsTemplate) {
					//if the target is node template, it must be compatible with the
					//node type specification in the requirement defintion
						String targetNodeType = (String)
								catalog.getTemplate(theContext.target(),Construct.Node,targetNode).get("type");
						if (!catalog.isDerivedFrom(
																Construct.Node, targetNodeType,targetNodeDef)) {
							theContext.addError(Message.INCOMPATIBLE_REQUIREMENT_TARGET, Construct.Node, targetNodeType + " of target node " + targetNode, targetNodeDef);
							return;
						}
					}
					else {
					//if the target is a node type it must be compatible (= or derived
					//from) with the node type specification in the requirement definition
						if (!catalog.isDerivedFrom(
																Construct.Node, targetNode, targetNodeDef)) {
							theContext.addError(Message.INCOMPATIBLE_REQUIREMENT_TARGET, Construct.Node, targetNode, targetNodeDef);
							return;
						}
					}
				}
			}

			String targetNodeType = targetNodeIsTemplate ?
					(String)catalog.getTemplate(theContext.target(),Construct.Node,targetNode).get("type"):
					targetNode;

			//capability assignment
      boolean targetCapabilityIsType = false;
			String targetCapability = (String)theAssignment.get("capability");
			if (targetCapability == null) {
				targetCapability = (String)theDefinition.get("capability");
				//in a requirement definition the target capability can only be a
				//capability type (and not a capability name within some target node
				//type)
				targetCapabilityIsType = true;
			}
			else {
        targetCapabilityIsType = isTypeReference(Construct.Capability, targetCapability);

				//check compatibility with the target compatibility type specified
				//in the requirement definition, if any
				String targetCapabilityDef = (String)theDefinition.get("capability");
				if (targetCapabilityDef != null && targetCapability != null) {
					if (targetCapabilityIsType) {
						if (!catalog.isDerivedFrom(
								Construct.Capability, targetCapability, targetCapabilityDef)) {
							theContext.addError(Message.INCOMPATIBLE_REQUIREMENT_TARGET, Construct.Capability, targetCapability, targetCapabilityDef);
							return;
						}
					}
					else {
						//the capability is from a target node. Find its definition and
						//check that its type is compatible with the capability type
						//from the requirement definition

						//check target capability compatibility with target node		
						if (targetNode == null) {
							theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', which was not specified", null);
							return;
						}
						if (!targetNodeIsTemplate) {
							theContext.addError("The capability '" + targetCapability + "' is not a capability type, hence it has to be a capability of the node template indicated in 'node', but there you specified a node type", null);
							return;
						}
						//check that the targetNode (its type) indeed has the 
						//targetCapability

						Map<String,Object> targetNodeCapabilityDef = 
																findTypeFacetByName(
																			Construct.Node, targetNodeType,
																			Facet.capabilities,	targetCapability);
						if (targetNodeCapabilityDef == null) {
							theContext.addError("No capability '" + targetCapability + "' was specified in the node " + targetNode + " of type " + targetNodeType, null);
							return;
						}

						String targetNodeCapabilityType = (String)targetNodeCapabilityDef.get("type");

						if (!catalog.isDerivedFrom(Construct.Capability,
																			 targetNodeCapabilityType,
																			 targetCapabilityDef)) {
							theContext.addError("The required target capability type '" + targetCapabilityDef + "' is not compatible with the target capability type found in the target node type capability definition : " + targetNodeCapabilityType + ", targetNode " + targetNode + ", capability name " + targetCapability, null);
							return;
						}
					}
			  }
			}
		
			//relationship assignment
			Map targetRelationship = (Map)theAssignment.get("relationship");
      if (targetRelationship != null) {
				//this has to be compatible with the relationship with the same name
				//from the node type
				//check the type
			}

			//node_filter; used jxpath to simplify the navigation somewhat
			//this is too cryptic
	  	JXPathContext jxPath = JXPathContext.newContext(theAssignment);
			jxPath.setLenient(true);

			List<Map> propertiesFilter = 
									(List<Map>)jxPath.getValue("/node_filter/properties");
			if (propertiesFilter != null) {
				for (Map propertyFilter: propertiesFilter) {
//System.out.println("propertiesFilter " + propertyFilter);
					
					if (targetNode != null) {
						//if we have a target node or node template then it must have
						//have these properties
						for (Object propertyName: propertyFilter.keySet()) {
							if (null == findTypeFacetByName(Construct.Node,
																						  targetNodeType,
																							Facet.properties,
																							propertyName.toString())) {
								theContext.addError("The node_filter property " + propertyName + " is invalid: requirement target node " + targetNode + " does not have such a property", null);
							}
						}
					}
					else if (targetCapability != null) {
			/*
						//if we have a target capability type (but not have a target node)
						//than it must have these properties

			Not true, the filter always refer to node properties: it is the processor's/orchestrator job to match the
			this requirement with a node that satisfies the filter. We cannot anticipate the values of all properties
      (some might come from inputs) so we cannot scan for candidates at this point.


						if (targetCapabilityIsType) {
							for (Object propertyName: propertyFilter.keySet()) {
								if (null == findTypeFacetByName(Construct.Capability,
																								targetCapability,
																								Facet.properties,
																								propertyName.toString())) {
									theContext.addError("The node_filter property " + propertyName + " is invalid: requirement target capability " + targetCapability + " does not have such a property", null);
								}
							}
						}
						else {
							//cannot be: if you point to an explicit capability then you must
							//have specified a targetNode
						}
				*/
					}
					else {
						//what are the properties suppose to filter on ??
					}
				}
			}

			List<Map> capabilitiesFilter = 
											(List<Map>)jxPath.getValue("node_filter/capabilities");
			if (capabilitiesFilter != null) {
				for (Map capabilityFilterDef: capabilitiesFilter) {
					assert capabilityFilterDef.size() == 1;
					Map.Entry<String, Map> capabilityFilterEntry = 
							(Map.Entry<String, Map>)capabilityFilterDef.entrySet().iterator().next();
					String targetFilterCapability = capabilityFilterEntry.getKey();
					Map<String,Object> targetFilterCapabilityDef = null;

					//if we have a targetNode capabilityName must be a capability of
					//that node (type); or it can be simply capability type (but the node
					//must have a capability of that type)

					String targetFilterCapabilityType = null;
					if (targetNode != null) {
						targetFilterCapabilityDef = 
								findTypeFacetByName(Construct.Node, targetNodeType,
																		Facet.capabilities, targetFilterCapability);
						if (targetFilterCapabilityDef != null) {
							targetFilterCapabilityType =
										(String)targetFilterCapabilityDef/*.values().iterator().next()*/.get("type");
						}
						else {
							Map<String,Map> targetFilterCapabilities =
								findTypeFacetByType(Construct.Node, targetNodeType,
																		Facet.capabilities, targetFilterCapability);
							
							if (!targetFilterCapabilities.isEmpty()) {
								if (targetFilterCapabilities.size() > 1) {
									log.warning("check_requirement_assignment_definition: filter check, target node type '" + targetNodeType + "' has more than one capability of type '" + targetFilterCapability + "', not supported");
								}
								//pick the first entry, it represents a capability of the required type
								Map.Entry<String,Map> capabilityEntry = targetFilterCapabilities.entrySet().iterator().next();
								targetFilterCapabilityDef = Collections.singletonMap(capabilityEntry.getKey(),
																																		 capabilityEntry.getValue());
								targetFilterCapabilityType = targetFilterCapability;
							}
						}
					}
					else {
						//no node (type) specified, it can be a straight capability type
						targetFilterCapabilityDef = catalog.getTypeDefinition(
																	Construct.Capability, targetFilterCapability);
						//here comes the odd part: it can still be a just a name in which
						//case we should look at the requirement definition, see which 
						//capability (type) it indicates
						assert targetCapabilityIsType; //cannot be otherwise, we'd need a node
						targetFilterCapabilityDef = catalog.getTypeDefinition(
																	Construct.Capability, targetCapability);
						targetFilterCapabilityType = targetCapability;
					}

					if (targetFilterCapabilityDef == null) {
						theContext.addError("Capability (name or type) " + targetFilterCapability + " is invalid: not a known capability (type) " + 
						((targetNodeType != null) ? (" of node type" + targetNodeType) : ""), null);
						continue;
					}

					for (Map propertyFilter: 
									(List<Map>)jxPath.getValue("/node_filter/capabilities/" + targetFilterCapability + "/properties")) {
						//check that the properties are in the scope of the 
						//capability definition
						for (Object propertyName: propertyFilter.keySet()) {
							if (null == findTypeFacetByName(Construct.Capability,
																							targetCapability,
																							Facet.properties,
																							propertyName.toString())) {
									theContext.addError("The capability filter " + targetFilterCapability + " property " + propertyName + " is invalid: target capability " + targetFilterCapabilityType + " does not have such a property", null);
							}
						}
					}
				}
			}

		}
		finally {
			theContext//.exit()
			          .exit();
		}
  }

	protected void check_capabilities_assignment_definition(
										Map<String,Map> theCapabilities, CheckContext theContext) {
		theContext.enter("capabilities");
    try {
			if(!checkDefinition("capabilities", theCapabilities, theContext))
		 		return;
			
			//the node type for the node template enclosing these requirements
			String nodeType =	(String)catalog.getTemplate(
															theContext.target(),
															Construct.Node,
															theContext.enclosingConstruct(Construct.Node).name())
																.get("type");

			for (Iterator<Map.Entry<String,Map>> ci =
											theCapabilities.entrySet().iterator();
			     ci.hasNext(); ) {
				
				Map.Entry<String,Map> ce = ci.next();

				String capabilityName = ce.getKey();
				Map		 capabilityDef = findTypeFacetByName(Construct.Node, nodeType,
																			Facet.capabilities, capabilityName);
				if (capabilityDef == null) {
					theContext.addError("No capability " + capabilityName + " was defined for the node type " + nodeType, null);
					continue;
				}

				check_capability_assignment_definition(
					capabilityName, ce.getValue(), capabilityDef,theContext);
			}
		}
		finally {
 	   theContext.exit();
		}
	}

	protected void check_capability_assignment_definition(
																			String theCapabilityName,
																			Map theAssignment,
																			Map theDefinition,
																			CheckContext theContext) {

		theContext.enter(theCapabilityName, Construct.Capability);
	  try {
			String capabilityType = (String)theDefinition.get("type");
			//list of property and attributes assignments
			checkFacet(Construct.Capability, theAssignment, capabilityType,
								 Facet.properties, theContext);
			checkFacet(Construct.Capability, theAssignment, capabilityType,
								 Facet.attributes, theContext);
		}
		finally {
			theContext.exit();
		}
	}

	/** */
	protected void check_template_interfaces_definition(
																				Map<String,Map> theInterfaces,
																				CheckContext theContext) {
		theContext.enter("interfaces");
    try {
			if(!checkDefinition("interfaces", theInterfaces, theContext))
		 		return;
			
			//the node type for the node template enclosing these requirements
			String nodeType =	(String)catalog.getTemplate(
															theContext.target(),
															Construct.Node,
															theContext.enclosingConstruct(Construct.Node).name())
																.get("type");

			for (Iterator<Map.Entry<String,Map>> ii =
											theInterfaces.entrySet().iterator();
			     ii.hasNext(); ) {
				
				Map.Entry<String,Map> ie = ii.next();

				String interfaceName = ie.getKey();
				Map		 interfaceDef = findTypeFacetByName(Construct.Node,	nodeType, 
																					Facet.interfaces, interfaceName);
				
				if (interfaceDef == null) {
					/* this is subject to augmentation: this could be a warning but not an error */	
					theContext.addError(Message.INVALID_INTERFACE_REFERENCE, nodeType, interfaceName, Construct.Node);
					continue;
				}

				check_template_interface_definition(
					interfaceName, ie.getValue(), interfaceDef, theContext);
			}
		}
		finally {
			theContext.exit();
		}
	}
	
	protected void check_template_interface_definition(
																			String theInterfaceName,
																			Map theAssignment,
																			Map theDefinition,
																			CheckContext theContext) {

		theContext.enter(theInterfaceName, Construct.Interface);
	  try {
			//check the assignment of the common inputs
//System.out.println("Checking interface inputs for " + theInterfaceName);
			checkFacet(Construct.Interface,
								 theAssignment,
								 (String)theDefinition.get("type"),
								 Facet.inputs,
								 theContext);

			//check the assignment of inputs in each operation
			//unfortunately operations are not defined as a facet (grouped under a
			//facet name) i.e. operations..
			
/*
			Map<String, Map> inputsDefs = theDefinition.get("inputs");
			Map<String,?> inputs = theAssignment.get("inputs");
			
			if (inputs != null && !inputs.isEmpty()) {
				for (Map.Entry inputEntry: input.entrySet()) {
					//check the input name part of the definition
					if (inputDefs != null && inputDefs.containsKey(inputEntry.getKey())) {
						checkDataValuation(inputEntry.getValue(),
															 inputsDefs.get(inputEntry.getKey()),
															 theContext);						
					}
					else {
						theContext.addError("No input " + inputEntry.getKey() + " was defined for the interface " + theInterfaceName, null);
					}
				}
			}
*/
/*
			String interfaceType = (String)theDefinition.get("type");
			//list of property and attributes assignments
			checkFacet(Construct.Interface, theAssignment, interfaceType,
								 "inputs", theContext);
*/
			//the interface operations: can new operations be defined here??
		}
		finally {
			theContext.exit();
		}
	}

	
	@Checks(path="/topology_template/artifacts")
	protected void check_template_artifacts_definition(
																				Map<String,Object> theDefinition,
																				CheckContext theContext) {		
		theContext.enter("artifacts");
    try {
		}
		finally {
			theContext.exit();
		}
	}
	
	protected void check_template_artifact_definition(
																			String theArtifactName,
																			Map theAssignment,
																			Map theDefinition,
																			CheckContext theContext) {

		theContext.enter(theArtifactName, Construct.Artifact);
	  try {

		}
		finally {
			theContext.exit();
		}
	}

  //generic checking actions, not related to validation rules


  /* the type can be:
   *   a known type: predefined or user-defined
   *   a collection (list or map) and then check that the entry_schema points to one of the first two cases (is that it?)
   */
  protected boolean checkDataType(
											String theName, Map theSpec, CheckContext theContext) {
	  
		if (!checkTypeReference(Construct.Data, theContext, (String)theSpec.get("type")))
      return false;

	  String type = (String)theSpec.get("type");
    if (/*isCollectionType(type)*/
        "list".equals(type) || "map".equals(type)) {
 	   	Map entry_schema = (Map)theSpec.get("entry_schema");
  	  if (entry_schema == null) {
      	//maybe issue a warning ?? or is 'string' the default??
       	return true;
     	}

			if (!catalog.hasType(Construct.Data,(String)entry_schema.get("type"))) {
       	theContext.addError("Unknown entry_schema type: " + entry_schema, null);
       	return false;
     	}
    }
    return true;
  }

  /*
	 * generic checks for a type specification
	 */
  protected boolean checkTypeConstruct(Construct theConstruct,
	                                     String theTypeName,
	                                     Map theDef,
																			 CheckContext theContext) {
		/* There is a 'weakness' in the super-type check before: the search for the supertype is done globally and 
		 * not strictly on the 'import' path, i.e. one should explore for the super-type definition the target sub-tree
		 * starting at the current target and not ALL the targets
		 */
		String parentType = (String)theDef.get("derived_from");
		if (parentType != null && !catalog.hasType(theConstruct, parentType)) {
			theContext.addError(
				Message.INVALID_TYPE_REFERENCE, "derived_from", parentType, theConstruct);
			return false;
		}
    return true;
  }

  /* Check that a particular facet (properties, attributes) of a construct type
	 * (node type, capability type, etc) is correctly (consistenly) defined
	 * across a type hierarchy
	 */
  protected boolean checkTypeConstructFacet(Construct theConstruct,
	                                          String theTypeName,
	                                          Map theTypeSpec,
	                                          Facet theFacet,
																			      CheckContext theContext) {
    Map<String, Map> defs =
		   	               (Map<String,Map>)theTypeSpec.get(theFacet.name());
    if (null == defs) {
      return true;
    }

    boolean res = true;

    //given that the type was cataloged there will be at least one entry
    Iterator<Map.Entry<String,Map>> i =
															catalog.hierarchy(theConstruct, theTypeName);
		if (!i.hasNext()) {
      theContext.addError(
			  "The type " + theTypeName + " needs to be cataloged before attempting 'checkTypeConstruct'", null);
			return false;
		}
    i.next(); //skip self
    while(i.hasNext()) {
      Map.Entry<String,Map> e = i.next();
      Map<String, Map> superDefs = (Map<String,Map>)e.getValue()
																												.get(theFacet.name());
      if (null == superDefs) {
        continue;
      }
      //this computes entries that appear on both collections but with different values, i.e. the re-defined properties
      Map<String, MapDifference.ValueDifference<Map>> diff = Maps.difference(defs, superDefs).entriesDiffering();
      
      for (Iterator<Map.Entry<String, MapDifference.ValueDifference<Map>>> di = diff.entrySet().iterator(); di.hasNext(); ) {
         Map.Entry<String, MapDifference.ValueDifference<Map>> de = di.next();
         MapDifference.ValueDifference<Map> dediff = de.getValue();
         log.finest(
           theConstruct + " type " + theFacet + ": " + de.getKey() + " has been re-defined between the " + theConstruct + " types " + e.getKey() + " and " + theTypeName);
         //for now we just check that the type is consistenly re-declared
         //if (!dediff.leftValue().get("type").equals(dediff.rightValue().get("type"))) {
         if (!this.catalog.isDerivedFrom(theFacet.construct(),
												 						(String)dediff.leftValue().get("type"),
												 						(String)dediff.rightValue().get("type"))) {
           theContext.addError(
					  theConstruct + " type " + theFacet + ", redefiniton changed its type: "+ de.getKey() + " has been re-defined between the " + theConstruct + " types " + e.getKey() + " and " + theTypeName + " in an incompatible manner", null);
           res = false;  
         }
      }
    }

    return res;   
  }

  /*
	 * Checks the validity of a certain facet of a construct
	 * (properties of a node) across a type hierarchy.
   * For now the check is limited to a verifying that a a facet was declared
	 * somewhere in the construct type hierarchy (a node template property has
	 * been declared in the node type hierarchy).
	 *
	 * 2 versions with the more generic allowing the specification of the type
	 * to be done explicitly.
   */
  protected boolean checkFacet(Construct theConstruct,
															 Map theSpec,
															 Facet theFacet,
															 CheckContext theContext) {
		return checkFacet(theConstruct, theSpec, null, theFacet, theContext);
	}
	
	/**
	 * We walk the hierarchy and verify the assignment of a property with respect to its definition.
	 * We also collect the names of those properties defined as required but for which no assignment was provided.
 	 */
	protected boolean checkFacet(Construct theConstruct,
															 Map theSpec,
															 String theSpecType,
															 Facet theFacet,
															 CheckContext theContext) {

    Map<String,Map> defs = (Map<String,Map>)theSpec.get(theFacet.name());
    if (null == defs) {
      return true;
    }
    defs = Maps.newHashMap(defs); //

    boolean res = true;
		if (theSpecType == null) {
			theSpecType = (String)theSpec.get("type");
    }
		if (theSpecType == null) {
       theContext.addError("No specification type available", null);
			return false;
		}

		Map<String,Byte> missed = new HashMap<String, Byte>();  //keeps track of the missing required properties, the value is 
																											 //false if a default was found along the hierarchy
    Iterator<Map.Entry<String,Map>> i =
													catalog.hierarchy(theConstruct, theSpecType);
    while (i.hasNext() && !defs.isEmpty()) {
      Map.Entry<String,Map> type = i.next();

      Map<String, Map> typeDefs = (Map<String,Map>)type.getValue()
																													.get(theFacet.name());
      if (null == typeDefs) {
        continue;
      }

      MapDifference<String, Map> diff = Maps.difference(defs, typeDefs);

			//this are the ones this type and the spec have in common (same key,
			//different values)
      Map<String, MapDifference.ValueDifference<Map>> facetDefs = 
																								diff.entriesDiffering();
			//TODO: this assumes the definition of the facet is not cumulative, i.e.
			//subtypes 'add' something to the definition provided by the super-types
			//it considers the most specialized definition stands on its own
			for (MapDifference.ValueDifference<Map> valdef: facetDefs.values()) {
				checkDataValuation(valdef.leftValue(), valdef.rightValue(), theContext);
			}

			//the ones that appear in the type but not in spec; ensure the type does not requires them.
/*
			Map<String, Map> unassigned = diff.entriesOnlyOnRight();
			for (Map.Entry<String, Map> unassignedEntry: unassigned.entrySet()) {

System.out.println(" **** unassigned -> " + unassignedEntry.getKey() + " : " + unassignedEntry.getValue());

				if (unassignedEntry.getValue().containsKey("required")) {
					Boolean required = (Boolean)unassignedEntry.getValue().get("required");
					System.out.println(" **** before " + unassignedEntry.getKey() + ", required " + required  + " = " + missed.get(unassignedEntry.getKey()));
					missed.compute(unassignedEntry.getKey(),
												 (k, v) -> v == null ? (required.booleanValue() ? (byte)1 
																																				: (byte)0)
																						 : (required.booleanValue() ? (byte)(v.byteValue() | 0x01)
																																				: (byte)(v.byteValue() & 0x02)));


					System.out.println(" **** after " + unassignedEntry.getKey() + ", required " + required  + " = " + missed.get(unassignedEntry.getKey()));
				}
				if (unassignedEntry.getValue().containsKey("default")) {
					System.out.println(" **** before " + unassignedEntry.getKey() + ", default = " + missed.get(unassignedEntry.getKey()));
					missed.compute(unassignedEntry.getKey(),
												 (k, v) -> v == null ? (byte)2
																						 : (byte)(v.byteValue() | 0x02));
					System.out.println(" **** after " + unassignedEntry.getKey() + ", default = " + missed.get(unassignedEntry.getKey()));
				}
			}
*/
      //remove from properties all those that appear in this type: unfortunately this returns an unmodifiable map ..
      defs = Maps.newHashMap(diff.entriesOnlyOnLeft());
    }

    if (!defs.isEmpty()) {
			theContext.addError(Message.INVALID_FACET_REFERENCE, theConstruct, theFacet, theSpecType, defs);
      res = false;
    }

		if (!missed.isEmpty()) {
			List missedNames = 
				missed.entrySet()
							.stream()
							.filter(e -> e.getValue().byteValue() == (byte)1)
							.map(e -> e.getKey())
							.collect(Collectors.toList());
			if (!missedNames.isEmpty()) {
      	theContext.addError(theConstruct + " " + theFacet + " missing required values for: " + missedNames, null);
       	res = false;
			}
		}

    return res;
  }

	/* Augmentation occurs in cases such as the declaration of capabilities within a node type.
   * In such cases the construct facets (the capabilitity's properties) can redefine (augment) the 
   * specification found in the construct type. 
   */
  protected boolean checkFacetAugmentation(Construct theConstruct,
															 						 Map theSpec,
															 						 Facet theFacet,
															 						 CheckContext theContext) {
		return checkFacetAugmentation(theConstruct, theSpec, null, theFacet, theContext);
	}

	protected boolean checkFacetAugmentation(Construct theConstruct,
															 						 Map theSpec,
																					 String theSpecType,
																					 Facet theFacet,
																					 CheckContext theContext) {
    
		Map<String,Map> augs = (Map<String,Map>)theSpec.get(theFacet.name());
    if (null == augs) {
      return true;
    }
    
    boolean res = true;
		if (theSpecType == null) {
			theSpecType = (String)theSpec.get("type");
    }
		if (theSpecType == null) {
       theContext.addError("No specification type available", null);
			return false;
		}

		for (Iterator<Map.Entry<String,Map>> ai = augs.entrySet().iterator(); ai.hasNext(); ) {
			Map.Entry<String,Map> ae = ai.next();
			
			//make sure it was declared by the type
			Map facetDef = catalog.getFacetDefinition(theConstruct, theSpecType, theFacet, ae.getKey());
			if (facetDef == null) {
       	theContext.addError("Unknown " + theConstruct + " " + theFacet + " (not declared by the type " + theSpecType + ") were used: " + ae.getKey(), null);
				res = false;
				continue;
			}

			//check the compatibility of the augmentation: only the type cannot be changed
			//can the type be changed in a compatible manner ??
			if (!facetDef.get("type").equals(ae.getValue().get("type"))) {
       	theContext.addError(theConstruct + " " + theFacet + " " + ae.getKey() + " has a different type than its definition: " + ae.getValue().get("type") + " instead of " + facetDef.get("type"), null);
				res = false;
				continue;
			}

			//check any valuation (here just defaults)
			Object defaultValue = ae.getValue().get("default");
			if (defaultValue != null) {
				checkDataValuation(defaultValue, ae.getValue(), theContext);
			}
		}

		return res;
	}
  
  protected boolean catalogTypes(Construct theConstruct, Map<String,Map> theTypes, CheckContext theContext) {

		boolean res = true;
		for (Map.Entry<String,Map> typeEntry: theTypes.entrySet()) {
			res &= catalogType(theConstruct, typeEntry.getKey(), typeEntry.getValue(), theContext); 
		}

		return res;
	}

  /* */
	protected boolean catalogType(Construct theConstruct,
															  String theName,
															  Map theDef,
															  CheckContext theContext) {
			  
		if (!catalog.addType(theConstruct, theName, theDef)) {
			theContext.addError(theConstruct + " type " + theName + " re-declaration", null);
			return false;
		}
		log.finer(theConstruct + " type " + theName + " has been cataloged");

		return true;
	}


  /* */
  protected boolean checkTypeReference(Construct theConstruct,
																			 CheckContext theContext,
																			 String... theTypeNames) {
		boolean res = true;
		for (String typeName: theTypeNames) {
			if (!isTypeReference(theConstruct, typeName)) {
				theContext.addError(Message.INVALID_TYPE_REFERENCE, "", typeName, theConstruct);
				res = false;
			}
		}
		return res;
	}

  protected boolean isTypeReference(Construct theConstruct,
																		String theTypeName) {
		return this.catalog.hasType(theConstruct, theTypeName);
  }
  
	/* node or relationship templates */
	protected boolean checkTemplateReference(Construct theConstruct,
																			     CheckContext theContext,
																			     String... theTemplateNames) {
		boolean res = true;
		for (String templateName: theTemplateNames) {
			if (!isTemplateReference(theConstruct, theContext, templateName)) {
				theContext.addError(Message.INVALID_TEMPLATE_REFERENCE, "", templateName, theConstruct);
				res = false;
			}
		}
		return res;
  }
  
	protected boolean catalogTemplates(Construct theConstruct,
																		 Map<String,Map> theTemplates,
																		 CheckContext theContext) {

		boolean res = true;
		for (Map.Entry<String,Map> typeEntry: theTemplates.entrySet()) {
			res &= catalogTemplate(theConstruct, typeEntry.getKey(), typeEntry.getValue(), theContext); 
		}

		return res;
	}

	protected boolean catalogTemplate(Construct theConstruct,
															  		String theName,
															  		Map theDef,
															  		CheckContext theContext) {
		try {
	  	catalog.addTemplate(theContext.target(), theConstruct, theName, theDef);
			log.finer(theConstruct + " " + theName + " has been cataloged");
		}
		catch(CatalogException cx) {
		  theContext.addError("Failed to catalog " + theConstruct + " " + theName, cx);
			return false;
		}
		return true;
	}

	protected boolean isTemplateReference(Construct theConstruct,
																				CheckContext theContext,
																				String theTemplateName) {
		return this.catalog.hasTemplate(theContext.target(),theConstruct, theTemplateName);
	}

	/*
	 * For inputs/properties/attributes/(parameters). It is the caller's
	 * responsability to provide the value (from a 'default', inlined, ..)
	 *
	 * @param theDef the definition of the given construct/facet as it appears in
	 * 			its enclosing type definition.
	 * @param 
	 */
	protected boolean checkDataValuation(Object theExpr,
																			 Map<String,?> theDef,
																			 CheckContext theContext) {
		//first check if the expression is a function, if not handle it as a value assignment
		Data.Function f = Data.function(theExpr);
		if (f != null) {
			return f.evaluator()
							 .eval(theExpr, theDef, theContext);
		}
		else {
			Data.Type type = Data.typeByName((String)theDef.get("type"));
			if (type != null) {
//System.out.println("Evaluating " + theExpr + " as " + theExpr.getClass().getName() + " against " + theDef);
				Data.Evaluator evaluator = null;

				evaluator = type.evaluator();
				if (evaluator == null) {
					log.info("No value evaluator available for type " + type);
				}
				else {
					if (theExpr != null) {
						if (!evaluator.eval(theExpr, theDef, theContext)) {
							return false;
						}
					}
				}
				
				evaluator = type.constraintsEvaluator();
				if (evaluator == null) {
					log.info("No constraints evaluator available for type " + type);
				}
				else { 
					if (theExpr != null) {
						if (!evaluator.eval(theExpr, theDef, theContext)) {
							return false;
						}
					}
					else {
						//should have a null value validator
					}
				}
	
				return true;
			}
			else {
				theContext.addError("Expression " + theExpr + " of " + theDef + " could not be evaluated", null);
				return false;
			}
		}
	}

	/** Given the type of a certain construct (node type for example), look up
	 * in one of its facets (properties, capabilities, ..) for one of the given
	 * facet type (if looking in property, one of the given data type).
	 * @return a map of all facets of the given type, will be empty to signal
	 * none found
	 *
	 * Should we look for a facet construct of a compatible type: any type derived
	 * from the given facet's construct type??
	 */
	protected Map<String,Map> 
									findTypeFacetByType(Construct theTypeConstruct,
																	    String theTypeName,
																			Facet theFacet,
																			String theFacetType) {
		
		log.logp(Level.FINER, "", "findTypeFacetByType", theTypeName + " " +  theTypeConstruct + ": " + theFacetType + " " + theFacet);
		Map<String,Map> res= new HashMap<String,Map>();
    Iterator<Map.Entry<String,Map>> i =
															catalog.hierarchy(theTypeConstruct, theTypeName);
		while (i.hasNext()) {
			Map.Entry<String,Map> typeSpec = i.next();
			log.logp(Level.FINER, "", "findTypeFacetByType", "Checking " + theTypeConstruct + " type " + typeSpec.getKey() );
			Map<String,Map> typeFacet =
									(Map<String,Map>)typeSpec.getValue().get(theFacet.name());
			if (typeFacet == null) {
				continue;
			}
    	Iterator<Map.Entry<String,Map>> fi = typeFacet.entrySet().iterator();
			while(fi.hasNext()) {
				Map.Entry<String,Map> facet = fi.next();
				String facetType = (String)facet.getValue().get("type");
				log.logp(Level.FINER, "", "findTypeFacetByType", "Checking " + facet.getKey() + " type " + facetType);

				//here is the question: do we look for an exact match or ..
				//now we check that the type has a capability of a type compatible
				//(equal or derived from) the given capability type.
				if (catalog.isDerivedFrom(
							theFacet.construct(), /*theFacetType, facetType*/facetType, theFacetType)) {
					//res.merge(facet.getKey(), facet.getValue(), (currDef, newDef)->(merge the base class definition in the existing definition but provide the result in a new map as to avoid changing the stored defintitions));
					res.putIfAbsent(facet.getKey(), facet.getValue());
				}
			}
		}
		log.logp(Level.FINER, "", "findTypeFacetByType", "found " + res);

    return res;
  }
	
	/* */
	protected Map<String,Object>
									findTypeFacetByName(Construct theTypeConstruct,
																	    String theTypeName,
																			Facet theFacet,
																			String theFacetName) {
		log.logp(Level.FINER, "", "findTypeFacetByName", theTypeConstruct + " " + theTypeName);
    Iterator<Map.Entry<String,Map>> i =
															catalog.hierarchy(theTypeConstruct, theTypeName);
		while (i.hasNext()) {
			Map.Entry<String,Map> typeSpec = i.next();
			log.logp(Level.FINER, "", "findTypeFacetByName", "Checking " + theTypeConstruct + " type " + typeSpec.getKey() );
			Map<String,Map> typeFacet =
											(Map<String,Map>)typeSpec.getValue().get(theFacet.name());
			if (typeFacet == null) {
				continue;
			}
			Map<String,Object> facet = typeFacet.get(theFacetName);
			if (facet != null) {
				return facet;
			}
		}
		return null;
	}

	/* Requirements are the odd ball as they are structured as a sequence ..
	 */	
	protected Map<String,Map> findNodeTypeRequirementByName(
																String theNodeType, String theRequirementName) {
		log.logp(Level.FINER, "", "findNodeTypeRequirementByName", theNodeType + "/" + theRequirementName);
    Iterator<Map.Entry<String,Map>> i =
															catalog.hierarchy(Construct.Node, theNodeType);
		while (i.hasNext()) {
			Map.Entry<String,Map> nodeType = i.next();
			log.logp(Level.FINER, "", "findNodeTypeRequirementByName", "Checking node type " + nodeType.getKey() );
			List<Map<String,Map>> nodeTypeRequirements =
							(List<Map<String,Map>>)nodeType.getValue().get("requirements");
			if (nodeTypeRequirements == null) {
				continue;
			}

			for (Map<String,Map> requirement: nodeTypeRequirements) {
				Map requirementDef = requirement.get(theRequirementName);
				if (requirementDef != null) {
					return requirementDef;
				}
			}
		}
		return null;
	}

	/*
	 * 
	 */
	public Map findNodeTemplateInterfaceOperation(
								Target theTarget, String theNodeName, String theInterfaceName, String theOperationName) {

		Map nodeDefinition = (Map)catalog.getTemplate(theTarget, Construct.Node, theNodeName);
		if (nodeDefinition == null)
			return null;

		Map interfaces = (Map)nodeDefinition.get("interfaces");
		if (interfaces == null)
			return null;

		Map interfaceDef = (Map)interfaces.get(theInterfaceName);
		if (interfaceDef == null)
			return null;

		return (Map)interfaceDef.get(theOperationName);
	}

	public Map findNodeTypeInterfaceOperation(
								String theNodeType, String theInterfaceName, String theOperationName) {
	
		return null;	
	}
	
  /*
	 * Assumes that at this time the constrints (syntax) for all names (construct
	 * types, constructs, facets: ) are the same.
	 */
	public boolean checkName(String theName,
												      CheckContext theContext) {
		return true;
	}
	
	/*
	 * Additional generics checks to be performed on any definition: construct,
	 * construct types, etc ..
	 */
	public boolean checkDefinition(String theName,
															Map theDefinition, 
												      CheckContext theContext) {
	  if (theDefinition == null) {
		  theContext.addError("Missing definition for " + theName, null);
				return false;
		}
	  
		if (theDefinition.isEmpty()) {
		  theContext.addError("Empty definition for " + theName, null);
				return false;
		}

		return true;
	}

	public boolean checkDefinition(String theName,
															List theDefinition, 
												      CheckContext theContext) {
	  if (theDefinition == null) {
		  theContext.addError("Missing definition for " + theName, null);
				return false;
		}
	  
		if (theDefinition.isEmpty()) {
		  theContext.addError("Empty definition for " + theName, null);
				return false;
		}

		return true;
	}

	/* I'd rather validate each import once at it's own rule time (see next method) but unfortunately the canonicals
	 * are not visible 'right away' (they are applied at the end of the pre-validation but not visible in the
	 * post-validation of the same rule because of kwalify validator implementation).
	 */
	@Validates(rule="service_template_definition",timing=Validates.Timing.post)
  protected void validate_imports(
			Object theValue, Rule theRule, Validator.ValidationContext theContext) {

		Map template = (Map)theValue;
		List<Map> imports = (List)template.get("imports");

		if (imports != null) {
			for (Map importEntry: imports) {
				validate_import(mapEntry(importEntry).getValue(), theRule, theContext);
			}
		}
	}

	//@Validates(rule="import_definition",timing=Validates.Timing.post)
  protected void validate_import(
			Object theValue, Rule theRule, Validator.ValidationContext theContext) {

	  log.entering("", "import", theContext.getPath());
					
		TOSCAValidator validator = (TOSCAValidator)theContext.getValidator();
		Target tgt = validator.getTarget();

		Map def = (Map)theValue; //importEntry.getValue(); 
		log.fine("Processing import " + def);

		String tfile = (String)def.get("file");
		Target tgti = this.locator.resolve(tfile);
		if (tgti == null) {
     	theContext.addError("Failure to resolve import '" + def + "', imported from " + tgt, theRule, null, null);
     	return; 
		}
		log.finer("Import " + def + " located at " + tgti.getLocation());
		
		if (this.catalog.addTarget(tgti, tgt)) {
	
			//we've never seen this import (location) before
			try {
				List<Target> tgtis = parseTarget(tgti);
				if (tgtis.isEmpty())
					return; //continue;

				if (tgtis.size() > 1) {
       		theContext.addError("Import '" + tgti + "', imported from " + tgt + ", contains multiple yaml documents" , theRule, null, null);
       		return; //continue;
				}

				tgti = tgtis.get(0);
				if (tgt.getReport().hasErrors()) {
       		theContext.addError("Failure parsing import '" + tgti + "',imported from " + tgt, theRule, null, null);
					return; //continue;
				}
        	
				validateTarget(tgti);
				if (tgt.getReport().hasErrors()) {
        	theContext.addError("Failure validating import '" + tgti + "',imported from " + tgt, theRule, null, null);
					return; //continue;
				}
      }
      catch (CheckerException cx) {
       	theContext.addError("Failure validating import '" + tgti + "',imported from " + tgt, theRule, cx, null);
      }
		}

		//replace with the actual location (also because this is what they get
		//index by .. bad, this exposed catalog inner workings)
		def.put("file", tgti.getLocation());
	}
	
	/* plenty of one entry maps around */
	private Map.Entry mapEntry(Object theMap) {
		return (Map.Entry)((Map)theMap).entrySet().iterator().next();
	}
 

	/* */
	protected static Catalog commonsCatalog = null;

	/*
	 * commons are built-in and supposed to be bulletproof so any error in here 
	 * goes out loud.
	 */
	protected static Catalog commonsCatalog() {
		
		synchronized (Catalog.class) {
			
			if (commonsCatalog != null) {
				return commonsCatalog;
			}

			//if other templates are going to be part of the common type system
			//add them to this list. order is relevant.
			final String[] commons = new String[] {
																	"tosca/tosca-common-types.yaml" };
		
			Checker commonsChecker = null;
			try {
				commonsChecker = new Checker();

				for (String common: commons) {
					commonsChecker.check(common, buildCatalog(false));
					Report commonsReport = commonsChecker.targets().iterator().next().getReport();

					if (commonsReport.hasErrors()) {
						throw new RuntimeException("Failed to process commons:\n" + 
																				commonsReport);
					}
				}
			}
			catch(CheckerException cx) {
				throw new RuntimeException("Failed to process commons", cx);
			}

			return commonsCatalog = commonsChecker.catalog;
		}
	}
  
	public static Catalog buildCatalog() {
		return buildCatalog(true);
	}

  /*
	 */
  public static Catalog buildCatalog(boolean doCommons) {

   	Catalog catalog = new Catalog(doCommons ? commonsCatalog() : null);
		if (!doCommons) {
    	//add core TOSCA types
			for (Data.CoreType type: Data.CoreType.class.getEnumConstants()) {
     		catalog.addType(Construct.Data, type.toString(), Collections.emptyMap());
			}
		}
		return catalog;
	}

	protected void checks(String theName,
 	                      Object theTarget,
											  CheckContext theContext) {

		handles("checks:" + theContext.getPath(theName), theTarget, theContext);
  }
	
	protected void catalogs(String theName,
 	                      	Object theTarget,
											  	CheckContext theContext) {

		handles("catalogs:" + theContext.getPath(theName), theTarget, theContext);
	}
	
	protected boolean validates(Validates.Timing theTiming,
	                 		        Object theTarget,
															Rule theRule,
															Validator.ValidationContext theContext) {
		//might look odd but we need both 'handles' call to be executed
		boolean validated = 
			handles(theTiming + "-validates:" + theRule.getName(), theTarget, theRule, theContext);
		return handles(theTiming + "-validates:", theTarget, theRule, theContext) || validated;
	}

	/*
	 * allow the handlers to return a boolean .. only do this in order to accomodate the Canonical's way of avoiding
	 * validation when a short form is encoutered.
	 * @return true if any handler returned true (if they returned something at all), false otherwise (even when no
   * handlers were found)
	 */
	protected boolean handles(String theHandlerKey,	Object... theArgs) {

		boolean handled = false;
		Map<Method, Object> entries = handlers.row(theHandlerKey);
		if (entries != null) {
			for (Map.Entry<Method, Object> entry: entries.entrySet()) {
				Object res = null;
				try {
					res = entry.getKey().invoke(entry.getValue(), theArgs);
				}
				catch (Exception x) {
       		log.log(Level.WARNING, theHandlerKey + " by " + entry.getKey() + " failed", x);
				}
				handled |= res == null ? false : (res instanceof Boolean && ((Boolean)res).booleanValue());
			}	
		}
		return handled;
	}

  /**
	 */
  public class TOSCAValidator extends Validator {

		//what were validating
   	private Target	target;
			
    public TOSCAValidator(Target theTarget, Object theSchema)
																										throws SchemaException {
      super(theSchema);
			this.target = theTarget;
    }

		public Target getTarget() {
			return this.target;
		}

    /* hook method called by Validator#validate() 
     */
    protected boolean preValidationHook(Object value, Rule rule, ValidationContext context) {

			return validates(Validates.Timing.pre, value, rule, context);
    }

    /*
     * Only gets invoked once the value was succesfully verified against the syntax indicated by the given rule.
     */
    protected void postValidationHook(Object value,
		                                  Rule rule,
																			ValidationContext context) {
      validates(Validates.Timing.post, value, rule, context);
    }

  }

  /** 
	 * Maintains state across the checking process.
	 */
  public class CheckContext {

		public class Step {

		  private final Construct construct;
 			private final String		name;
			private final Object		info;

  		public Step(String theName, Construct theConstruct, Object theInfo) {
    		this.construct = theConstruct;
    		this.name = theName;
    		this.info = theInfo;
  		}

  		public Construct 	construct() { return this.construct; }
  		public String 		name() { return this.name; }
  		public Object 		info() { return this.info; }
		}		


		private Target							target;
	  private ArrayList<Step>			steps = new ArrayList<Step>(20); //artificial max nesting ..


		public CheckContext(Target theTarget) {
			this.target = theTarget;
		}

    public CheckContext enter(String theName) {
    	return enter(theName, null, null);
		}

		public CheckContext enter(String theName, Construct theConstruct) {
			return enter(theName, theConstruct, null);
		}

		public CheckContext enter(String theName, Construct theConstruct, Object theInfo) {
			this.steps.add(new Step(theName, theConstruct, theInfo));
      Checker.this.log.entering("check", theName, getPath());
			return this;
		}
    
		public CheckContext exit() {
	 		Step step = this.steps.get(this.steps.size()-1);
      Checker.this.log.exiting("check", step.name(), getPath());
	 		this.steps.remove(this.steps.size()-1);
		  return this;
		}

		public String getPath() {
			return buildPath(null);
		}

		public String getPath(String theNextElem) {
			return buildPath(theNextElem);
		}

		protected String buildPath(String theElem) {
			StringBuffer sb = new StringBuffer("");
			for (Step s: this.steps)
				sb.append(s.name())
					.append("/");
			if (theElem != null)
				sb.append(theElem)
					.append("/");
			
			return sb.substring(0,sb.length()-1);
		}

    public Step enclosingConstruct(Construct theConstruct) {
			for (int i = this.steps.size()-1; i > 0; i--) {
			  Construct c = this.steps.get(i).construct();
				if (c != null && c.equals(theConstruct)) {
					return this.steps.get(i);
				}
			}
			return null;
		}
    
		public Step enclosingElement(String theName) {
			for (int i = this.steps.size()-1; i > 0; i--) {
			  String n = this.steps.get(i).name();
				if (n != null && n.equals(theName)) {
					return this.steps.get(i);
				}
			}
			return null;
		}

		public Step enclosing() {
			if (this.steps.size() > 0) {
				return this.steps.get(this.steps.size()-1);
			}
			return null;
		}

		public CheckContext addError(String theMessage, Throwable theCause) {
      this.target.report(new TargetError("", getPath(), theMessage, theCause));
			return this;
		}
		
		public CheckContext addError(Message theMsg, Object... theArgs) {
			this.target.report(new TargetError("", getPath(), messages.format(theMsg, theArgs), null));
			return this;
		}

    public boolean hasErrors() {
			return this.target.getReport().hasErrors();
		}

		public Checker checker() {
			return Checker.this;
		}

		public Catalog catalog() {
			return Checker.this.catalog;
		}

		public Target target() {
			return this.target;
		}

		public String toString() {
			return "CheckContext(" + this.target.getLocation() + "," + getPath() + ")";
		}
	}

	public static class CheckerConfiguration {

		private boolean 			allowAugmentation = true;
		private String				defaultImportsPath = null;
		private List<String>	grammars = new LinkedList<String>();

		protected CheckerConfiguration() {
			withGrammars(Checker.grammarFiles);
		}

		public CheckerConfiguration allowAugmentation(boolean doAllow) {
			this.allowAugmentation = doAllow;
			return this;
		}

		public boolean allowAugmentation() {
			return this.allowAugmentation;
		}

		public CheckerConfiguration withDefaultImportsPath(String thePath) {
			this.defaultImportsPath = thePath;
			return this;
		}

		public String defaultImportsPath() {
			return this.defaultImportsPath;
		}

		public CheckerConfiguration withGrammars(String... theGrammars) {
			for (String grammar: theGrammars) {
				this.grammars.add(grammar);
			}
			return this;
		}

		public String[] grammars() {
			return this.grammars.toArray(new String[0]);
		}
	}

}

