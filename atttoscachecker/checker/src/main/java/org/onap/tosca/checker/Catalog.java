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

import java.util.Iterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;

import java.util.stream.Collectors;

import java.net.URI;

import com.google.common.base.Predicate;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

/*
 * Oddball: tracking inputs as data templates could be seen as rather
 * odd but we see them as instances of data types, in the same way node 
 * templates are instances of node types.
 */
public class Catalog {

   /* Type hierarchies are stored as maps from a type name to its definition
    * Not the best but easy to follow hierarchies towards their root ..
    */
   private EnumMap<Construct, Map<String,Map>> types =
                            new EnumMap<Construct, Map<String,Map>>(Construct.class);
	 /* track templates: we track templates (tye instances) first per target then per contruct.
   	* This allows us to share the catalog among multiple templates sharign the same type set
    */
   private Map<Target, EnumMap<Construct, Map<String,Map>>> templates =
                            new HashMap<Target, EnumMap<Construct, Map<String,Map>>>();

	private Catalog parent;

	public Catalog(Catalog theParent) {
		this.parent = theParent;
		/* there are no requirement types, they are the same as capability types */
 		types.put(Construct.Data, new LinkedHashMap<String, Map>());
 		types.put(Construct.Capability, new LinkedHashMap<String, Map>());
 		types.put(Construct.Relationship, new LinkedHashMap<String, Map>());
 		types.put(Construct.Artifact, new LinkedHashMap<String, Map>());
		types.put(Construct.Interface, new LinkedHashMap<String, Map>());
		types.put(Construct.Node, new LinkedHashMap<String, Map>());
		types.put(Construct.Group, new LinkedHashMap<String, Map>());
		types.put(Construct.Policy, new LinkedHashMap<String, Map>());
     
	}
	
	public Catalog() {
		this(null);
	}

	public boolean addType(Construct theConstruct, String theName, Map theDef) {
		if (hasType(theConstruct, theName)) {
			return false;
		}
		getConstructTypes(theConstruct).put(theName, theDef);
		return true;
  }

	public Map getTypeDefinition(Construct theConstruct, String theName) {
		Map<String, Map> constructTypes = getConstructTypes(theConstruct);
		Map typeDef = constructTypes.get(theName);
		if (typeDef == null && this.parent != null) {
			return this.parent.getTypeDefinition(theConstruct, theName);
		}
		return typeDef;
	}

  public boolean hasType(Construct theConstruct, String theName) {
		Map<String, Map> constructTypes = getConstructTypes(theConstruct);
		boolean res = constructTypes.containsKey(theName);
		if (!res && this.parent != null) {
			res = this.parent.hasType(theConstruct, theName);
		}
		return res;
	}

	public Map<String, Map> getConstructTypes(Construct theConstruct) {
		Map<String, Map> constructTypes = this.types.get(theConstruct);
		if (null == constructTypes) {
			throw new RuntimeException("Something worse is cooking here!",
							new CatalogException("No types for construct " + theConstruct));
		}
		return constructTypes;
	}

	protected Iterator<Map.Entry<String,Map>> 
											typesIterator(Construct theConstruct) {
 		List<Map.Entry<String,Map>> constructTypes =
												new ArrayList<Map.Entry<String,Map>>(
													this.types.get(theConstruct).entrySet());
		Collections.reverse(constructTypes);
		return (this.parent == null)
							? constructTypes.iterator()
							: Iterators.concat(constructTypes.iterator(),
																 this.parent.typesIterator(theConstruct));
	}

   /* this will iterate through the type hierarchy for the given type, included.
    */
	public Iterator<Map.Entry<String,Map>>
										hierarchy(Construct theConstruct, final String theName) {
		return Iterators.filter(typesIterator(theConstruct),
                             new Predicate<Map.Entry<String,Map>>() {
                               Object next = theName;
                               public boolean apply(Map.Entry<String,Map> theEntry) {
                                 if (next != null && next.equals(theEntry.getKey())) {
                                   next = theEntry.getValue().get("derived_from");
                                   return true;
                                 }
                                 else
                                   return false;
                               }
                             });
   }
   
	 public boolean isDerivedFrom(Construct theConstruct, String theType, String theBaseType) {

		Iterator<Map.Entry<String,Map>> hierachyIterator = 
																					hierarchy(theConstruct, theType);
		while (hierachyIterator.hasNext())	{
			Map.Entry<String,Map> typeDef = hierachyIterator.next();

			if (typeDef.getKey().equals(theBaseType)) {
				return true;
			}
		}
		return  false;
	 }

	/* We go over the type hierarchy and retain only an iterator over the
	 * elements of the given facet for each type in the hierarchy.
	 * We concatenate these iterators and filter out duplicates.
	 * TODO: cannot just filter out duplicates - a redefinition can refine the one in the base construct so we
	 * should merge them!
	 */
	public Iterator<Map.Entry> facets(Construct theConstruct,
																		final Facet theFacet,
																		final String theName) {
		return
			Iterators.filter(	
				Iterators.concat(
					Iterators.transform(
						hierarchy(theConstruct, theName),
						new Function<Map.Entry<String,Map>, Iterator<Map.Entry>>() {
							public Iterator<Map.Entry> apply(Map.Entry<String,Map> theEntry) {
								Map m = (Map)theEntry.getValue().get(theFacet.name());
								return m == null
													? Collections.emptyIterator()
													: m.entrySet().iterator();
							}
						}							
					)
				),
        new Predicate<Map.Entry>() {
					Set insts = new HashSet();
					public boolean apply(Map.Entry theEntry) {
						return !insts.contains(theEntry.getKey());
					}
				}	
			);
	}
	
	//no need to specify a construct, only nodes can have requirements
	public Iterator<Map.Entry> requirements(final String theName) {
		return
			Iterators.concat(
				Iterators.transform(
					hierarchy(Construct.Node, theName),
					new Function<Map.Entry<String,Map>, Iterator<Map.Entry>>() {
						public Iterator<Map.Entry> apply(Map.Entry<String,Map> theEntry) {
							List<Map> l = (List<Map>)theEntry.getValue().get("requirements");
							return l == null
												? Collections.emptyIterator()
												: Iterators.concat(
														Iterators.transform(
															l.iterator(),
															new Function<Map, Iterator<Map.Entry>> () {
																public Iterator<Map.Entry> apply(Map theEntry) {
																	return theEntry.entrySet().iterator();
																}
															}
														)	
													);
							}
						}							
					)
				);
	}

	/* Example: find the definition of property 'port' of the node type 
	 * tosca.nodes.Database (properties being a facet of the node construct)
	 *
	 * Note: the definition of a facet is cumulative, i.e. more specialized
	 * definitions contribute (by overwriting) to the 
	 */
	public Map getFacetDefinition(Construct theConstruct,
																String theConstructTypeName,
																Facet theFacet,
																String theName) {
		Map def = null;
		Iterator<Map.Entry<String,Map>> ti = hierarchy(theConstruct, theConstructTypeName);
		while (ti.hasNext()) {
			//this is where requirements would yield a List ..
			Map<String,Map> fset = (Map<String,Map>)
															//theFacet.iterator(
														ti.next().getValue().get(theFacet.name());
			if (fset != null) {
				def = def == null ? fset.get(theName)
													: mergeDefinitions(def, fset.get(theName));
			}
		}
		return def;
	}

	public Map getRequirementDefinition(Construct theConstruct,
																			String theConstructTypeName,
																			String theName) {
		Iterator<Map.Entry<String,Map>> ti = hierarchy(theConstruct, theConstructTypeName);
		while (ti.hasNext()) {
			//this is where requirements yield a List ..
			List<Map> reqs = (List<Map>)
																ti.next().getValue().get("requirements");
			for (Map req: reqs) {
				Map.Entry reqe = (Map.Entry)req.entrySet().iterator().next();
				if (theName.equals(reqe.getKey())) {
					return (Map)reqe.getValue();
				}
			}
		}
		return null;
	}

  /* */
  private EnumMap<Construct,Map<String,Map>> getTemplates(Target theTarget) {
  	EnumMap<Construct, Map<String,Map>> targetTemplates = templates.get(theTarget);
		if (targetTemplates == null) {
			targetTemplates = new EnumMap<Construct,Map<String,Map>>(Construct.class);
			targetTemplates.put(Construct.Data, new LinkedHashMap<String, Map>());
			targetTemplates.put(Construct.Relationship, new LinkedHashMap<String, Map>());
			targetTemplates.put(Construct.Node, new LinkedHashMap<String, Map>());
			targetTemplates.put(Construct.Group, new LinkedHashMap<String, Map>());
			targetTemplates.put(Construct.Policy, new LinkedHashMap<String, Map>());

			templates.put(theTarget, targetTemplates);
		}
		return targetTemplates;
	}

	public Map<String,Map> getTargetTemplates(Target theTarget, Construct theConstruct) {
		return getTemplates(theTarget).get(theConstruct);
	}

	public void addTemplate(Target theTarget, Construct theConstruct, String theName, Map theDef) 
	 																								throws CatalogException {
  	Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
		if (null == constructTemplates) {
      throw new CatalogException("No such thing as " + theConstruct + " templates");
		}
    if (constructTemplates.containsKey(theName)) {
      throw new CatalogException(theConstruct + " template '" + theName + "' re-declaration");
    }
    constructTemplates.put(theName, theDef);
  }

  public boolean hasTemplate(Target theTarget, Construct theConstruct, String theName) {
    Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
    return constructTemplates != null && 
	        constructTemplates.containsKey(theName);
  }

  public Map getTemplate(Target theTarget, Construct theConstruct, String theName) {
    Map<String, Map> constructTemplates = getTargetTemplates(theTarget, theConstruct);
    if (constructTemplates != null)
		 	return constructTemplates.get(theName);
		else
			return null;
  }

	public static Map mergeDefinitions(Map theAggregate, Map theIncrement) {
		if (theIncrement == null)
			return theAggregate;

		for(Map.Entry e: (Set<Map.Entry>)theIncrement.entrySet()) {
			theAggregate.putIfAbsent(e.getKey(), e.getValue());
		}
		return theAggregate;
	}

  /* tracks imports, i.e.targets */
 	private LinkedHashMap<URI, Target> targets = 
	 													new LinkedHashMap<URI, Target>();
	/* tracks dependencies between targets, i.e. the 'adjency' matrix defined by
	 * the 'import' relationship */
	private Table<Target,Target,Boolean> imports = HashBasedTable.create();


	/*
   * theParent contains an 'include/import' statement pointing to the Target
   */
	public boolean addTarget(Target theTarget, Target theParent) {
		boolean cataloged = hasTarget(theTarget.getLocation());
	
		if(!cataloged) {
			 targets.put(theTarget.getLocation(), theTarget);
		}

		if (theParent != null) {
			imports.put(theParent, theTarget, Boolean.TRUE);
		}

		return !cataloged;
	}

	public boolean hasTarget(URI theLocation) {
		return this.targets.containsKey(theLocation) ? true
																								 : this.parent != null ? this.parent.hasTarget(theLocation)
																																			 : false;
	}

	public Target getTarget(URI theLocation) {
		Target t = this.targets.get(theLocation);
		if (t == null && this.parent != null) {
			t = this.parent.getTarget(theLocation);
		}
		return t;
	}

	public Collection<Target> targets() {
		return targets.values();
	}

	/* Targets that no other targets depend on */
	public Collection<Target> topTargets() {
		return targets.values()
						.stream()
							.filter(t -> !imports.containsColumn(t))
							.collect(Collectors.toList());
							
	}
	
	public String importString(Target theTarget) {
		return importString(theTarget, "  ");
	}

	private String importString(Target theTarget, String thePrefix) {
		StringBuilder sb = new StringBuilder("");
		Map<Target,Boolean> parents = imports.column(theTarget);
		if (parents != null) {
			for (Target p: parents.keySet()) {
				sb.append(thePrefix)
					.append("from ")
				  .append(p.getLocation())
					.append("\n")
					.append(importString(p, thePrefix + "  "));
			}
			//we only keep the positive relationships
		}
		return sb.toString();
	}

	/* */
	private class TargetComparator implements Comparator<Target> {
	
		/* @return 1 if there is a dependency path from TargetOne to TargetTwo, -1 otherwise  */
		public int compare(Target theTargetOne, Target theTargetTwo) {
			if (hasPath(theTargetTwo, theTargetOne))
				return -1;

			if (hasPath(theTargetOne, theTargetTwo))
				return 1;

			return 0;
		}

		public boolean hasPath(Target theStart, Target theEnd) {
			Map<Target,Boolean> deps = imports.row(theStart);
			if (deps.containsKey(theEnd))
				return true;
			for (Target dep: deps.keySet()) {
				if (hasPath(dep, theEnd))
					return true;
			}
			return false;
		}
	}

	public Collection<Target> sortedTargets() {
		List keys = new ArrayList(this.targets.values());
		Collections.sort(keys, new TargetComparator());
		return keys;		
	}

	public static void main(String[] theArgs) throws Exception {

		Catalog cat = new Catalog();

		Target a = new Target("a", new URI("a")),
				b = new Target("b", new URI("b")),
				c = new Target("c", new URI("c")),
				d = new Target("d", new URI("d"));

		cat.addTarget(a, null);
		cat.addTarget(b, null);
		cat.addTarget(c, null);
		cat.addTarget(d, null);

		cat.addTarget(b, c);
		cat.addTarget(a, c);
		cat.addTarget(c, d);
		cat.addTarget(a, b);

		//System.out.println(cat.importString(c));
		for (Target t: cat.sortedTargets())
			System.out.println(t);

		Catalog root = new Catalog();
		root.addType(Construct.Node, "_a", Collections.emptyMap());
		root.addType(Construct.Node, "__a", Collections.singletonMap("derived_from", "_a"));
		root.addType(Construct.Node, "___a", Collections.singletonMap("derived_from", "_a"));

		Catalog base = new Catalog(root);
		base.addType(Construct.Node, "_b", Collections.singletonMap("derived_from", "__a"));
		base.addType(Construct.Node, "__b", Collections.singletonMap("derived_from", "_b"));
		base.addType(Construct.Node, "__b_", Collections.singletonMap("derived_from", "_a"));

		if (theArgs.length > 0) {
			Iterator<Map.Entry<String, Map>> ti = 
																	base.hierarchy(Construct.Node, theArgs[0]);
			while (ti.hasNext()) {
				System.out.println("> " + ti.next().getKey());
			}
		}
	}
}
