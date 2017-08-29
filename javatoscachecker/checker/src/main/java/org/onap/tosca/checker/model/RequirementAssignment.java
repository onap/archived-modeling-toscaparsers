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
package org.onap.tosca.checker.model;

/*
 * Requirement assignment as it appears in node templates. See spec section 3.7.2
 */
public interface RequirementAssignment extends TOSCAObject<RequirementAssignment> {

	public String name();

	/**
	 * Provide the name of either a:
	 * 	Capability definition within a target node template that can fulfill the requirement.
	 * 	Capability Type that the provider will use to select a type-compatible target node template to fulfill the requirement at runtime.
	 */
	public String capability();

	/**
	 */
	public String node();
	
	/** */
	public default NodeFilter node_filter() {
		return (NodeFilter)proxy("node_filter", NodeFilter.class);
	}

	/** */
	public default RelationshipAssignment relationship() {
		return (RelationshipAssignment)proxy("relationship", RelationshipAssignment.class);
	}

	public interface RelationshipAssignment extends TOSCAObject<RelationshipAssignment> {

		public String type();

		public default PropertiesAssignments properties() {
			return (PropertiesAssignments)proxy("properties", PropertiesAssignments.class);
		}

		public default TemplateInterfaces interfaces() {
			return (TemplateInterfaces)proxy("interfaces", TemplateInterfaces.class);
		}
	}
}
