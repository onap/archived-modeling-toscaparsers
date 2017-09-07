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

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.EnumSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

/*
 * tosca data type (primitive or user defined) evaluators and validators, used in checking valuations
 */
public class Data {

	private static Logger log = Logger.getLogger(Data.class.getName());


	private Data() {
	}
	
	/*
	 */
	@FunctionalInterface
	public static interface Evaluator {

		public boolean eval(Object theExpr, Map theDef, Checker.CheckContext theCtx);	
	}


  /* data type processing */

	private static Map<String,Type>	typesByName = new HashMap<String,Type>();
	static {
		//CoreType.String.toString();
		//CoreFunction.concat.toString();
		//Constraint.equal.toString();
	}


	public static Data.Type typeByName(String theName) {
		return typesByName.getOrDefault(theName, userType);
	}
/*
	public static Evaluator getTypeEvaluator(Type theType) {
	}
*/

	/* Needs a better name ?? RValue?? 
	 * This is not an rvalue (C def) per se but the construct who's instances
	 * yield rvalues. It is a construct that yields data, not the data (yield)
	 * itself.
	 */
	public static interface Type {
		
		public String name();

		public boolean isScalar();

		public Evaluator evaluator();

		public Evaluator constraintsEvaluator();
	}

	/* generic placeholder
	 */
	private static Type userType = new Type() {
	
		public String name() {
			return null;
		}

		public boolean isScalar() {
			return false;
		}

		public Evaluator evaluator() {
			return Data::evalUser;
		}

		public Evaluator constraintsEvaluator() {
			return Data::evalUserConstraints;
		}
		
		public String toString() {
			return "UserType(scalar=false)";
		}
	};


	public static enum CoreType implements Type {

    String("string", true,
					(expr,def,ctx) -> expr != null && expr instanceof String,
					Data::evalScalarConstraints),
    Integer("integer", true,
					(expr,def,ctx) -> Data.valueOf(ctx, expr, Integer.class),
					Data::evalScalarConstraints),
		Float("float", true,
					(expr,def,ctx) -> Data.valueOf(ctx, expr, Double.class, Integer.class),
					Data::evalScalarConstraints),
 	  Boolean("boolean", true,
					(expr,def,ctx) -> Data.valueOf(ctx, expr, Boolean.class),
					Data::evalScalarConstraints),
 	  Null("null", true,
					(expr,def,ctx) -> expr.equals("null"),
					null),
   	Timestamp("timestamp", true,
					(expr,def,ctx) -> timestampRegex.matcher(expr.toString()).matches(),
					null),
    List("list", false, Data::evalList, Data::evalListConstraints),
    Map("map", false, Data::evalMap, Data::evalMapConstraints),
		Version("version", true,
					(expr,def,ctx) -> versionRegex.matcher(expr.toString()).matches(),
					null),
		/* use a scanner and check that the upper bound is indeed greater than
		 * the lower bound. Marked as a scalar because its evaluator does not record a context error */
    Range("range", true,
					(expr,def,ctx) -> { return rangeRegex.matcher(expr.toString()).matches();},
					null ),
    Size("scalar-unit.size", true,
				 (expr,def,ctx) -> sizeRegex.matcher(expr.toString()).matches(),
					null),
    Time("scalar-unit.time", true,
				 (expr,def,ctx) -> timeRegex.matcher(expr.toString()).matches(),
					null),
    Frequency("scalar-unit.frequency", true,
				 (expr,def,ctx) -> frequencyRegex.matcher(expr.toString()).matches(),
					null);


		private String 		toscaName;
		private boolean		isScalar;
		private Evaluator	valueEvaluator,
											constraintsEvaluator;

		private CoreType(String theName,
										 boolean theIsScalar,
										 Evaluator theValueEvaluator,
										 Evaluator theConstraintsEvaluator) {
			this.toscaName = theName;
			this.isScalar = theIsScalar;
			this.valueEvaluator = theValueEvaluator;
			this.constraintsEvaluator = theConstraintsEvaluator;

			if (typesByName == null)
				throw new RuntimeException("No type index available!");

			typesByName.put(this.toscaName, this);
		}

		public String toString() {
			return this.toscaName;
		}

		public boolean isScalar() {
			return this.isScalar;
		}

		public Evaluator evaluator() {
			return this.valueEvaluator;
		}
		
		public Evaluator constraintsEvaluator() {
			return this.constraintsEvaluator;
		}

	}

	private static Pattern timestampRegex = null,
												 versionRegex = null,
												 rangeRegex = null,
												 sizeRegex = null,
												 timeRegex = null,
												 frequencyRegex = null;

	static {
		try {
			timestampRegex = Pattern.compile(
					"\\p{Digit}+"); //?? where to find the definition

			//<major_version>.<minor_version>[.<fix_version>[.<qualifier>[-<build_version]]]
			versionRegex = Pattern.compile(
					"\\p{Digit}+\\.\\p{Digit}+?(\\.\\p{Digit}+(\\.\\p{Alpha}+(\\-\\p{Digit}+))*)*");
			
			rangeRegex = Pattern.compile(
					"\\[[ ]*\\p{Digit}+(\\.\\p{Digit}+)?[ ]*\\,[ ]*(\\p{Digit}+(\\.\\p{Digit}+)?|UNBOUNDED)[ ]*\\]");
			
			sizeRegex = Pattern.compile(
					"\\p{Digit}+(\\.\\p{Digit}+)?[ ]*(B|kB|KiB|MB|MiB|GB|GiB|TB|TiB)");
			
			timeRegex = Pattern.compile(
					"\\p{Digit}+(\\.\\p{Digit}+)?[ ]*(d|h|m|s|ms|us|ns)");
			
			frequencyRegex = Pattern.compile(
					"\\p{Digit}+(\\.\\p{Digit}+)?[ ]*(Hz|kHz|MHz|GHz)");
		}
		catch (PatternSyntaxException psx) {
			throw new RuntimeException("Bad patterns", psx);
		}
	}

	/* */	
	public static boolean evalScalarConstraints(Object theVal,
																 							Map theDef,
																 							Checker.CheckContext theCtx) {
		Data.Type type = typeByName((String)theDef.get("type"));
		List<Map<String,Object>> constraints =
															(List<Map<String,Object>>)theDef.get("constraints");
		if (constraints == null) {
			return true;
		}

		//check value against constraints
		boolean res = true;
		for (Map<String,Object> constraintDef: constraints) {
			Map.Entry<String,Object> constraintEntry =
																		constraintDef.entrySet().iterator().next();
			Data.Constraint constraint = constraintByName(constraintEntry.getKey());

// the def passed here includes all constraints, not necessary! we can pass
// simple constraintEntry.getValue()
			Evaluator constraintEvaluator = getTypeConstraintEvaluator(type, constraint);
			if (constraintEvaluator == null) {
				log.info("No constaint evaluator available for " + type + "/" + constraint);
				continue;
			}

			if (!constraintEvaluator.eval(theVal, theDef, theCtx)) {
				theCtx.addError("Value " + theVal + " failed constraint " + constraintEntry, null);
				res = false;
			}
		}
		return res;
	}

	/*
	 * It assumes the specification is complete, i.e. it contains a valid
	 * entry_schema section.
	 * TODO: check constraints, i.e. entrySchema.get("constraints")
	 */
	public static boolean evalList(Object theVal,
																 Map theDef,
																 Checker.CheckContext theCtx) {
		try {
			return evalCollection((List)theVal, theDef, theCtx);
		}
		catch (ClassCastException ccx) {
			theCtx.addError("Value " + theVal + " not a list", null);
			return false;
		}
	}
	
	public static boolean evalMap(Object theVal,
																Map theDef,
																Checker.CheckContext theCtx) {
		try {
			return evalCollection(((Map)theVal).values(), theDef, theCtx);
		}
		catch (ClassCastException ccx) {
			theCtx.addError("Value " + theVal + " not a map", null);
			return false;
		}
	}


	/**
	 * The elements of a collection can be of a core type or user defined type.
   */
	private static boolean evalCollection(Collection theVals,
																 				Map theDef,
																 				Checker.CheckContext theCtx) {

//System.out.println("evalCollection: " + theDef + ", " + theVals);

		Data.Type entryType = null;
		Map entryTypeDef = (Map)theDef.get("entry_schema");
		if (null != entryTypeDef)
			entryType = typeByName((String)entryTypeDef.get("type"));

//System.out.println("evalCollection, entry definition: " + entryTypeDef);
		boolean res = true;
		for (Object val: theVals) {
			//check if the value is not a function call
			Data.Function f = Data.function(val);
			if (f != null &&
					f.evaluator().eval(val, entryTypeDef, theCtx)) {
				res = false;
			}
			else if (entryType != null &&
							!entryType.evaluator().eval(val, entryTypeDef, theCtx)) {
				res= false;
				//the error should hav been reported by the particular evaluator
				//theCtx.addError("Value " + val + " failed evaluation", null);
			}
		}
		return res;
	}
	
	public static boolean evalListConstraints(Object theVal,
																 						Map theDef,
																 						Checker.CheckContext theCtx) {
		return evalCollectionConstraints((List)theVal, theDef, theCtx);
	}
	
	public static boolean evalMapConstraints(Object theVal,
																					 Map theDef,
																					 Checker.CheckContext theCtx) {
		return evalCollectionConstraints(((Map)theVal).values(), theDef, theCtx);
	}

	private static boolean evalCollectionConstraints(Collection theVals,
																 									 Map theDef,
																 									 Checker.CheckContext theCtx) {
//System.out.println("evalCollectionConstraints: " + theDef + ", " + theVals);

		//should check overall constraints 

		if (theVals == null)
			return true;

		Map entryTypeDef = (Map)theDef.get("entry_schema");
		if (null == entryTypeDef)
			return true;

		String entryTypeName = (String)entryTypeDef.get("type");
		Data.Type entryType = typeByName(entryTypeName);

//System.out.println("evalCollectionConstraints, entry definition: " + entryTypeDef);

		boolean res = true;
		for (Object val: theVals) {
			Evaluator entryEvaluator = entryType.constraintsEvaluator();
			if (entryEvaluator != null &&
					!entryEvaluator.eval(val, entryTypeDef, theCtx)) {
				res= false;
				if (entryType.isScalar()) {
					theCtx.addError("Value " + val + " failed constraints evaluation", null);
				}
			}
		}
		return res;
	}

	/*
	 * All required properties across the hierarchical defintion must be present
	 * TODO:  The expr cannot contain any entry not specified in the type definition
	 */
	public static boolean evalUser(Object theVal,
																 Map theDef,
																 Checker.CheckContext theCtx) {
//System.out.println("evalUser: " + theDef + ", " + theVal);

		boolean res = true;
		Map val = (Map)theVal;
		//must be done with respect to the super-type(s) definition
		Iterator<Map.Entry> props =	theCtx.catalog()
																				.facets(Construct.Data,
																								Facet.properties,
																					 			(String)theDef.get("type"));	
		while (props.hasNext()) {
			Map.Entry propEntry = props.next();
			Map propDef = (Map)propEntry.getValue();
			Object propVal = val.get(propEntry.getKey());

			if (propVal != null) {
				Data.Type propType = typeByName((String)propDef.get("type"));

//System.out.println("evalUser: " + propVal + " of type " + propType + "/" + propType.isScalar());

				if (!propType.evaluator().eval(propVal, propDef, theCtx)) {
					res= false;
					if (propType.isScalar()) {
						theCtx.addError("Property " + propEntry.getKey() + " failed evaluation for " + propVal + " as " + propType.name(), null);
					}
				}
			}
		}
		return res;
	}
	
	public static boolean evalUserConstraints(Object theVal,
																 						Map theDef,
																 						Checker.CheckContext theCtx) {
		boolean res = true;
		Map val = (Map)theVal;
		Iterator<Map.Entry> props =	theCtx.catalog()
																				.facets(Construct.Data,
																								Facet.properties,
																					 			(String)theDef.get("type"));	
		while (props.hasNext()) {
			Map.Entry propEntry = props.next();
			Map propDef = (Map)propEntry.getValue();
			Object propVal = val.get(propEntry.getKey());
		
			if (propVal != null) {
				Data.Type propType = typeByName((String)propDef.get("type"));

				if (propType.constraintsEvaluator() != null &&
						!propType.constraintsEvaluator().eval(propVal, propDef, theCtx)) {
					res= false;
					//the constraints evaluator should have already added an error
					//theCtx.addError("Property " + propEntry.getKey() + " failed evaluation for " + propVal, null);
				}
			}
			else {
				if (Boolean.TRUE == (Boolean)propDef.getOrDefault("required", Boolean.FALSE) &&
						!propDef.containsKey("default")) {
					theCtx.addError("Property " + propEntry.getKey() + " failed 'required' constraint; definition is " + propDef, null);
					res = false;
				}
			}	
		}
		return res;
	}

	private static boolean valueOf(Checker.CheckContext theCtx,
																 Object theExpr,
																 Class ... theTypes) {
		for (Class type: theTypes) {
			if (type.isAssignableFrom(theExpr.getClass())) {
				return true;
			}
		}
				
		theCtx.addError("Expression " + theExpr + " as " + theExpr.getClass().getName() + " is not compatible with any of required types: " + Arrays.toString(theTypes), null);
		return false;
	}

/*
	private static boolean valueOf(Class theTarget,
																 String theExpr,
																 Checker.CheckContext theCtx) {
		try {
			theTarget.getMethod("valueOf", new Class[] {String.class})
								.invoke(null, theExpr);
			return true;
		}
		catch (InvocationTargetException itx) {
			theCtx.addError("Failed to parse " + theExpr + " as a " + theTarget.getName(), itx.getCause());
			return false;
		}
		catch (Exception x) {
			theCtx.addError("Failed to valueOf " + theExpr + " as a " + theTarget.getName(), x);
			return false;
		}
	}
*/

	/* 
	 * Function e(valuation)
	 * ?
	 * note to self : is there a more efficient way of retrieving a map's 
	 * single entry? (without knowing the key)
	 *
	 * ! Function evaluators have to handle null definition (i.e. perform argument checking) so that
	 * we can use them in the  context of collections with without entry_schemas
	 */
	
	//just as Type but is it worth expressing this 'commonality'??
	
	public static interface Function {
		
		public String name();

		public Evaluator evaluator();
	}

	/*
	 * This is a heuristic induced from the tosca specification .. it answers the
	 * question of wether the given expression is a function
	 */
	public static Function function(Object theExpr) {
		if (theExpr instanceof Map &&
				((Map)theExpr).size() == 1) {
			try {
				return Enum.valueOf(CoreFunction.class,	functionName(theExpr));
			}
			catch (IllegalArgumentException iax) {
				//no such function but we cannot really record an error as we only guessed the expression as being a function ..
				log.info("Failed attempt to interpret " + theExpr + " as a function call");
			}
		}
		
		return null;
	}

	/*
	 */
	public static String functionName(Object theExpr) {
		return	(String)
							((Map.Entry)
								((Map)theExpr).entrySet().iterator().next())
									.getKey();
	}

	/*
	 */
	public static Data.Function functionByName(String theName) {
		return Enum.valueOf(CoreFunction.class, theName);
	}

	/*
	 */	
	public static enum CoreFunction implements Function {

		concat(Data::evalConcat),
		token(Data::evalToken),
		get_input(Data::evalGetInput),
		get_property(Data::evalGetProperty),
		get_attribute(Data::evalGetAttribute),
		get_operation_output((expr,def,ctx) -> true),
		get_nodes_of_type(Data::evalGetNodesOfType),
		get_artifact((expr,def,ctx) -> true);
				 
		private Evaluator	evaluator;

		private CoreFunction(Evaluator theEval) {
			this.evaluator = theEval;
		}
		
		public Evaluator evaluator() {
			return this.evaluator;
		}
	}

	private static boolean evalConcat(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		return true;
	}
	
	private static boolean evalToken(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		return true;
	}

	private static boolean evalGetInput(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		Map val = (Map)theVal;
		Map.Entry entry = (Map.Entry)val.entrySet().iterator().next();

		if (!(entry.getValue() instanceof String)) {
			theCtx.addError("get_input: argument must be a String" ,null);
			return false;
		}

		//check that an input with the given name exists and has a compatible type
		Map inputDef = theCtx.catalog()
													.getTemplate(theCtx.target(), Construct.Data, (String)entry.getValue());
		if (inputDef == null) {
			theCtx.addError("get_input: no such input " + entry.getValue(), null);
			return false;
		}

		if (theDef == null)
			return true;

		//the output must be type compatible with the input
		String targetType = (String)theDef.get("type");
		if (targetType != null) {
			String inputType = (String)inputDef.get("type");

			if (!theCtx.catalog()
									.isDerivedFrom(Construct.Data, inputType, targetType)) {
				theCtx.addError("get_input: input type " + inputType + " is incompatible with the target type " + targetType, null);
				return false;
			}
		}

		return true;
	}

	/*
	 * Who's the smarty that decided to define optional arguments in between
	 * required ones ?!
	 * (factors the evaluation of get_attribute and get_property)
	 */
	private static boolean evalGetData(
						Object theVal, Map theDef,
						EnumSet<Facet> theFacets, Checker.CheckContext theCtx) {

		Map val = (Map)theVal;
		Map.Entry entry = (Map.Entry)val.entrySet().iterator().next();

		if (!(entry.getValue() instanceof List)) {
			theCtx.addError("get_property: argument must be a List" ,null);
			return false;
		}

		List args = (List)entry.getValue();
		if (args.size() < 2) {
			theCtx.addError("'get_property' has at least 2 arguments", null);
			return false;
		}

		//the first argument is a node or relationship template
		String 		tmpl = (String)args.get(0);
		Construct	tmplConstruct = null;
		Map		 		tmplSpec = null;

		if ("SELF".equals(tmpl)) {
			tmpl = theCtx.enclosingConstruct(Construct.Node).name();
			if (tmpl == null) {
				tmpl = theCtx.enclosingConstruct(Construct.Relationship).name();
				if (tmpl == null) {
					theCtx.addError("'get_property' invalid SELF reference: no node or relationship template in scope at " + theCtx.getPath(), null);
					return false;
				}
				else {
					tmplConstruct = Construct.Relationship;
				}
			}
			else {
				tmplConstruct = Construct.Node;
			}
			tmplSpec = theCtx.catalog().getTemplate(theCtx.target(), tmplConstruct, tmpl);
		}
		else if ("SOURCE".equals("tmpl")) {
			//we are in the scope of a relationship template and this is the source node template.
			tmpl = theCtx.enclosingConstruct(Construct.Relationship).name();
			if (tmpl == null) {
				theCtx.addError("'get_property' invalid SOURCE reference: no relationship template in scope at " + theCtx.getPath(), null);
				return false;
			}
			
			return true;
		}
		else if ("TARGET".equals("tmpl")) {
			//we are in the scope of a relationship template and this is the target node template.
			tmpl = theCtx.enclosingConstruct(Construct.Relationship).name();
			if (tmpl == null) {
				theCtx.addError("'get_property' invalid TARGET reference: no relationship template in scope at " + theCtx.getPath(), null);
				return false;
			}

			return true;
		}
		else if ("HOST".equals("tmpl")) {
			tmpl = theCtx.enclosingConstruct(Construct.Node).name();
			if (tmpl == null) {
				theCtx.addError("'get_property' invalid HOST reference: no node template in scope at " + theCtx.getPath(), null);
				return false;
			}

			return true;
		}
		else {
			//try node template first
			tmplSpec = theCtx.catalog().getTemplate(theCtx.target(), Construct.Node, tmpl);
			if (tmplSpec == null) {
				//try relationship
				tmplSpec = theCtx.catalog().getTemplate(theCtx.target(), Construct.Relationship, tmpl);
				if (tmplSpec == null) {
					theCtx.addError("'get_data' invalid template reference '" + tmpl + "': no node or relationship template with this name", null);
					return false;
				}
				else {
					tmplConstruct = Construct.Relationship;
				}
			}
			else {
				tmplConstruct = Construct.Node;
			}
		}

		int facetNameIndex = 1;
		Construct facetConstruct = tmplConstruct; //who's construct the facet is supposed to belong to
		Map 			facetConstructSpec = null;
		String 		facetConstructType = null;

		if (tmplConstruct.equals(Construct.Node) &&
				args.size() > 2) {
			//the second arg might be a capability or requirement name. If it is a
			//capability than the third argument becomes a property of the
			//coresponding capability type. If it is a requirement than the
			//requirement definition indicates a capability who's type has a
			//property with the name indicated in the third argument ..
			//
			//while the spec does not make it explicit this can only take place
			//if the first argument turned out to be a node template (as relationship
			//templates/types do not have capabilities/requirements
			String secondArg = (String)args.get(1);
			if ((facetConstructSpec = theCtx.catalog().getFacetDefinition(
																						tmplConstruct,
														 								(String)tmplSpec.get("type"),
																						Facet.capabilities,
																						secondArg)) != null) {
				facetNameIndex = 2;
				facetConstruct = Construct.Capability;
				facetConstructType = (String)facetConstructSpec.get("type");
			}
			else if ((facetConstructSpec = theCtx.catalog().getRequirementDefinition(
																							tmplConstruct,
																							(String)tmplSpec.get("type"),
																							secondArg)) != null) {	
				facetNameIndex = 2;
				facetConstruct = Construct.Capability;
				
				//find the specof the capability this requirement points to
				//TODO: check, can the capability reference be anything else but a capability tyep?
				facetConstructType = (String)facetConstructSpec.get("capability");
			}
		}
		else {
			//we'll attempt to handle it as a property of the node template
			facetConstruct = Construct.Node;
			facetConstructSpec = tmplSpec;
			facetConstructType = (String)facetConstructSpec.get("type");
		}
	
		//validate the facet name
		Map facetSpec = null;
		{
			String facetName = (String)args.get(facetNameIndex);
			for (Facet facet: theFacets) {
//System.out.println("get_data: looking at " + facetConstruct + " " + facetConstructType + ", " + facet + " " + facetName);
				facetSpec = theCtx.catalog()
											.getFacetDefinition(
														facetConstruct,
														facetConstructType,
										  			facet,
														facetName);
				if (facetSpec != null)
					break;
			}

			if (facetSpec == null) {
//TODO: not the greatest message if the call strated with a requirement ..
				theCtx.addError("'get_data' invalid reference, '" + facetConstruct + "' " + facetConstructType + " has no " + theFacets + " with name " + facetName, null);
				return false;
			}
		}

		//the rest of the arguments have to resolve to a field of the property's
		//data type; the propertySpec contains the type specification
		for (int i = facetNameIndex + 1; i < args.size(); i++)  {
		}

		return true;
	}
	
	/**/
	private static boolean evalGetProperty(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		return evalGetData(theVal, theDef, EnumSet.of(Facet.properties), theCtx);
	}

	/*
	 * get_property and get_attribute are identical, just operating on different
	 * facets, with one exception: there is an intrinsec attribute for every 
	 * declared property.
	 */
	private static boolean evalGetAttribute(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		return evalGetData(theVal, theDef, EnumSet.of(Facet.attributes, Facet.properties), theCtx);
	}

	private static boolean evalGetNodesOfType(
						Object theVal, Map theDef, Checker.CheckContext theCtx) {
		
		Map val = (Map)theVal;
		Map.Entry entry = (Map.Entry)val.entrySet().iterator().next();

		if (!(entry.getValue() instanceof String)) {
			theCtx.addError("get_nodes_of_type: argument must be a String", null);
			return false;
		}

		String arg = (String)entry.getValue();

		if (null == theCtx.catalog().getTypeDefinition(Construct.Node, arg)) {
			theCtx.addError("get_nodes_of_type: no such node type " + arg, null);
			return false;
		}
		else {
			return true;
		}
	}

	/* */
	public static Constraint constraintByName(String theName) {
		return Enum.valueOf(Constraint.class,	theName);
	}

	/* */	
	public static Constraint constraint(Object theExpr) {
		if (theExpr instanceof Map &&
				((Map)theExpr).size() == 1) {
			return constraintByName(constraintName(theExpr));
		}
		
		return null;
	}

	/* */
	public static String constraintName(Object theExpr) {
		return	(String)
							((Map.Entry)
								((Map)theExpr).entrySet().iterator().next())
									.getKey();
	}

	private static Object getConstraintValue(Map theDef,
																					 Constraint theConstraint) {
		List<Map> constraints = (List<Map>)theDef.get("constraints");
		if (null == constraints)
			return null;

		for(Map constraint: constraints) {
			Object val = constraint.get(theConstraint.toString());
			if (val != null)
				return val;
		}
		return null;
	}

	public static enum Constraint {
		equal,
		greater_than,
		greater_or_equal,
		less_than,
		less_or_equal,
		in_range,
		valid_values,
		length,
		min_length,
		max_length,
		pattern;
	}


	/* hold the constraint evaluators for pairs of type/constraint.
	 * If a pair is not present than the given constraint does not apply
	 * to the type.
	 */
	private static Table<Type,Constraint,Evaluator> typeConstraintEvaluator =null;

	public static Evaluator
						getTypeConstraintEvaluator(Type theType, Constraint theConstraint) {
		if (typeConstraintEvaluator == null) {
			typeConstraintEvaluator = HashBasedTable.create();

			typeConstraintEvaluator.put(CoreType.String, Constraint.equal,
				(val,def,ctx) -> val.equals(getConstraintValue(def,Constraint.equal)));
			typeConstraintEvaluator.put(CoreType.String, Constraint.valid_values,
				(val,def,ctx) -> {
						return ((List)getConstraintValue(def,Constraint.valid_values)).contains(val);
			  });
			typeConstraintEvaluator.put(CoreType.String, Constraint.length,
				(val,def,ctx) -> ((String)val).length() == ((Number)getConstraintValue(def,Constraint.length)).intValue());
			typeConstraintEvaluator.put(CoreType.String, Constraint.min_length, 
				(val,def,ctx) -> ((String)val).length() >= ((Number)getConstraintValue(def,Constraint.min_length)).intValue());
			typeConstraintEvaluator.put(CoreType.String, Constraint.max_length, 
				(val,def,ctx) -> ((String)val).length() <= ((Number)getConstraintValue(def,Constraint.max_length)).intValue());
			typeConstraintEvaluator.put(CoreType.String, Constraint.pattern,
				(val,def,ctx) -> Pattern.compile((String)getConstraintValue(def,Constraint.pattern))
																	.matcher((String)val)
																		.matches());

			typeConstraintEvaluator.put(CoreType.Integer, Constraint.equal,
				(val,def,ctx) -> ((Integer)val).compareTo((Integer)getConstraintValue(def,Constraint.equal)) == 0);
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.greater_than,
				(val,def,ctx) -> ((Integer)val).compareTo((Integer)getConstraintValue(def,Constraint.greater_than)) > 0);
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.greater_or_equal,
				(val,def,ctx) -> ((Integer)val).compareTo((Integer)getConstraintValue(def,Constraint.greater_or_equal)) >= 0);
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.less_than,
				(val,def,ctx) -> ((Integer)val).compareTo((Integer)getConstraintValue(def,Constraint.less_than)) < 0);
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.less_or_equal,
				(val,def,ctx) -> ((Integer)val).compareTo((Integer)getConstraintValue(def,Constraint.less_or_equal)) <= 0);
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.in_range,
				(val,def,ctx) -> { List<Integer> range = (List<Integer>)getConstraintValue(def, Constraint.in_range);
													 return ((Integer)val).compareTo(range.get(0)) >= 0 &&
																	((Integer)val).compareTo(range.get(1)) <= 0;
												});
			typeConstraintEvaluator.put(CoreType.Integer, Constraint.valid_values,
				(val,def,ctx) -> ((List<Integer>)getConstraintValue(def, Constraint.valid_values)).contains((Integer)val));
		
//yaml parser represents yaml floats as java Double and we are even more tolerant as many double values
//get represented as ints and the parser will substitute an Integer	
			typeConstraintEvaluator.put(CoreType.Float, Constraint.equal,
				(val,def,ctx) -> ((Number)val).doubleValue() == ((Number)getConstraintValue(def,Constraint.equal)).doubleValue());
			typeConstraintEvaluator.put(CoreType.Float, Constraint.greater_than,
				(val,def,ctx) -> ((Number)val).doubleValue() > ((Number)getConstraintValue(def,Constraint.greater_than)).doubleValue());
			typeConstraintEvaluator.put(CoreType.Float, Constraint.greater_or_equal,
				(val,def,ctx) -> ((Number)val).doubleValue() >= ((Number)getConstraintValue(def,Constraint.greater_or_equal)).doubleValue());
			typeConstraintEvaluator.put(CoreType.Float, Constraint.less_than,
				(val,def,ctx) -> ((Number)val).doubleValue() < ((Number)getConstraintValue(def,Constraint.less_than)).doubleValue());
			typeConstraintEvaluator.put(CoreType.Float, Constraint.less_or_equal,
				(val,def,ctx) -> ((Number)val).doubleValue() <= ((Number)getConstraintValue(def,Constraint.less_or_equal)).doubleValue());
			typeConstraintEvaluator.put(CoreType.Float, Constraint.in_range,
				(val,def,ctx) -> { List<Number> range = (List<Number>)getConstraintValue(def, Constraint.in_range);
													 return ((Number)val).doubleValue() >= range.get(0).doubleValue() &&
																	((Number)val).doubleValue() <= range.get(1).doubleValue();
												});
			typeConstraintEvaluator.put(CoreType.Float, Constraint.valid_values,
				(val,def,ctx) -> ((List<Number>)getConstraintValue(def, Constraint.valid_values)).contains((Number)val));
		}

		Evaluator eval = typeConstraintEvaluator.get(theType, theConstraint);

		return eval == null ? (expr,def,ctx) -> true
												: eval;
	}

	
	private static boolean stringValidValues(String theVal,
																 					 List<String> theValidValues,
																 					 Checker.CheckContext theCtx) {
		if (!theValidValues.contains(theVal)) {
			theCtx.addError("not a valid value: " + theVal + " not part of " + theValidValues, null);
			return false;
		}

		return true;
	}

	public static final void main(String[] theArgs) {
		Data.CoreType dt = Enum.valueOf(Data.CoreType.class, theArgs[0]);
		System.out.println(theArgs[1] + " > " + dt.evaluator().eval(theArgs[1], null, null));
	}
}
