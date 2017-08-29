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

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import org.onap.tosca.checker.annotations.Checks;

/**
 * Workflows checking isolated as it is of significant size. Showcases the way checking can be added to the core
 * checker through annotations.
 */
@Checks
public class Workflows {

	@Checks(path="/topology_template/workflows")
	public void check_workflows(Map theDefinition, Checker.CheckContext theContext) {

		theContext.enter("workflows", null, theDefinition);
	
		try {
			if(!theContext.checker().checkDefinition("workflows", theDefinition, theContext))
		 		return;

  	  for (Iterator<Map.Entry<String,Map>> i = theDefinition.entrySet().iterator(); i.hasNext(); ) {
    	  Map.Entry<String,Map> e = i.next();
				check_workflow_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}


	public void check_workflow_definition(String theName, Map theDef, Checker.CheckContext theContext) {
		
		theContext.enter("workflow", Construct.Workflow);

		if (theDef.containsKey("inputs")) {
    	theContext
				.checker()
					.check_properties((Map<String,Map>)theDef.get("inputs"), theContext);
    }

		if (theDef.containsKey("preconditions")) {
			check_workflow_preconditions_definition((List<Map>)theDef.get("preconditions"), theContext);
		}

		if (theDef.containsKey("steps")) {
			check_workflow_steps_definition((Map<String, Map>)theDef.get("steps"), theContext);
		}

		theContext.exit();
	}
  	  

	public void check_workflow_steps_definition(Map theSteps, Checker.CheckContext theContext) {
		
		theContext.enter("steps");

		try {
			for (Iterator<Map.Entry<String,Map>> i = theSteps.entrySet().iterator(); i.hasNext(); ) {
  	  	Map.Entry<String,Map> e = i.next();
				check_workflow_step_definition(e.getKey(), e.getValue(), theContext);
    	}
		}
		finally {
			theContext.exit();
		}

	}

	public void check_workflow_step_definition(String theName, Map theDef, Checker.CheckContext theContext) {

		theContext.enter(theName, null, theDef);
		try {
			//target
			check_target_definition(theDef, theContext);

			//operation_host

			//filter: sequence of constraints with workflow assertion definitions (see section 3.5.18)
			//where the keys are attribute names: representing the name of an attribute defined on the assertion context
			//entity (node instance, relationship instance, group instance) 
 			//we could verify that all th eattributes ae defined .. how accurate can that be 

			//activities
			List<Map> activities = (List)theDef.get("activities");
			if (activities != null) {
				for (Map activity: activities) {
					check_workflow_step_activity_definition(activity, theContext);
				}
			}

			List successSteps = (List)theDef.get("on_success");
			List failureSteps = (List)theDef.get("on_failure");
			if (successSteps != null || failureSteps != null) {
				Map steps = (Map)theContext.catalog().getTemplate(theContext.target(),
														 														  Construct.Workflow,
																													theContext.enclosingConstruct(Construct.Workflow).name())
																								.get("steps");

				if (successSteps != null) {
					for (Object successStep: successSteps) {
						if (!steps.containsKey(successStep)) {
							theContext.addError("The 'on_success' step " + successStep + " was not declared", null);
						}
					}
				}

				if (failureSteps != null) {
					for (Object failureStep: failureSteps) {
						if (!steps.containsKey(failureStep)) {
							theContext.addError("The 'on_failure' step " + failureStep + " was not declared", null);
						}
					}
				}
			}
		}
		finally {
			theContext.exit();
		}
	}

	public void check_workflow_preconditions_definition(List<Map> thePreconditions, Checker.CheckContext theContext) {
		
		theContext.enter("preconditions");

		try {
			for (Map precondition: thePreconditions) {
				check_workflow_precondition_definition(precondition, theContext);
    	}
		}
		finally {
			theContext.exit();
		}
	}

	public void check_workflow_precondition_definition(Map theDef, Checker.CheckContext theContext) {
	
		Map targetDef = check_target_definition(theDef, theContext);
		if (targetDef != null) {
			List<Map> condDef = (List<Map>)theDef.get("condition");
			if (condDef != null) {
				check_condition_clause_definition(targetDef, condDef, theContext);
			}
		}
	}

	public void check_workflow_step_activity_definition(Map theDef, Checker.CheckContext theContext) {
		if (theDef.size() != 1)
			theContext.addError("An activity has only one entry", null);

		//only one entry expected
		String delegate = (String)theDef.get("delegate");
		if (delegate != null) {
			//not clear to me what this is:  a reference to a declarative workflow?
		}

		String state = (String)theDef.get("set_state");
		if (state != null) {
		}

		String operation = (String)theDef.get("call_operation");
		if (operation != null) {
			int pos = operation.lastIndexOf('.');
			if (pos < 0) {
				theContext.addError("Invalid 'operation_call' statement", null);
			}
			else {
				String interfaceName = operation.substring(0, pos),
							 interfaceOp = operation.substring(pos+1);

				//we expect the enclosing to be the workflow step, where the target is specified
				String stepTarget = (String)((Map)theContext.enclosing().info()).get("target");
				Paths  paths = theContext.checker().getHandler(Paths.class);
				Map		 opDef = null;
		
				//look in the node template
				if (null == (opDef = paths.nodeTemplateInterfaceOperation(
																										theContext.target(),
																										stepTarget,
																										interfaceName,
																										interfaceOp))) {
					//look in the node type
					if (null == (opDef = paths.nodeTypeInterfaceOperation(
																								paths.nodeTemplateType(theContext.target(), stepTarget),
																								interfaceName,
																								interfaceOp))) {
						
						//look into the interface type definition, if the node has an interface with the given name
						if (null == 
									(opDef = paths.interfaceTypeOperation(
																paths.nodeTypeInterfaceType(paths.nodeTemplateType(theContext.target(), stepTarget),
																												 		interfaceName),
																interfaceOp))) {
							theContext.addError("Step target node " + stepTarget + " does not have a " + interfaceName + " with operation " + interfaceOp, null);
						}
					}
				}

				System.out.println("*** opDef: " + opDef);
			}
		}

		String workflow = (String)theDef.get("inline");
		if (workflow != null && !((Map)theContext.enclosingElement("workflows").info()).containsKey(workflow)) {
			theContext.addError("Activity 'inline'  " + workflow + " was not declared", null);
		}
	}

	/* it would have been great to be ab le to convey in the return value the type of construct
	 */
	protected Map check_target_definition(Map theDef, Checker.CheckContext theContext) {
			
		String 		target = (String)theDef.get("target");
		Construct targetConstruct = null;
		Map				targetDef = null;

		if ((targetDef = theContext.catalog().getTemplate(theContext.target(), Construct.Group, target)) != null) {
			targetConstruct = Construct.Group;
		}
		else if ((targetDef = theContext.catalog().getTemplate(theContext.target(), Construct.Node, target)) != null) {
			targetConstruct = Construct.Node;
		}
		else {
			theContext.addError("The 'target' entry must contain a reference to a node template or group template, '" + target + "' is none of those", null);
		}

		String targetRelationship = (String)theDef.get("target_relationship");
		if (targetRelationship != null) {
			if (targetConstruct.equals(Construct.Node)) {
				//'The optional name of a requirement of the target in case the step refers to a relationship rather than a node or group'
				if (null == theContext.catalog().getRequirementDefinition(Construct.Node, 
																																	(String)targetDef.get("type"),
																																	targetRelationship)) {
					theContext.addError("The 'target' (" + target + ") has no requirement '" + targetRelationship + "', as indicated by 'targetRelationship'", null);
				}
			}
			else {
				theContext.addError("The 'target_relationship' is only applicable if the 'target' (" + target + ") is a node template (which it is not)", null);
			}
		}

		return targetDef;
	}

	protected boolean check_condition_clause_definition(Map theTargetDefinition,
																											List<Map> theConditionDef,
																											Checker.CheckContext theContext) {
		boolean res = true;

		for (Map clause: theConditionDef) {
			Map.Entry clauseEntry = (Map.Entry)clause.entrySet().iterator().next();
			String	  clauseType = (String)clauseEntry.getKey();

			if ("and".equals(clauseType) || "or".equals(clauseType)) {
				res &= check_condition_clause_definition(theTargetDefinition, (List<Map>)clauseEntry.getValue(), theContext);
			}
			else if ("assert".equals(clauseType)) {
				List<Map> assertions = (List<Map>)clauseEntry.getValue();
				for (Map assertion: assertions) {
					//expected to be a one entry map
					for (Object attributeName: assertion.keySet()) {
						//the attribute must be defined in the given Target .. but there are also exposed built-in attributes (such as 'state') that we are not aware of
					}
				}
			}
		}

		return res;
	}
}

