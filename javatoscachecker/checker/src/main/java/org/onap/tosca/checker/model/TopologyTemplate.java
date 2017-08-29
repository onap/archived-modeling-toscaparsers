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


public interface TopologyTemplate extends TOSCAObject<TopologyTemplate> {

	public String description();

	public default Inputs inputs() {
		return (Inputs)proxy("inputs", Inputs.class);
	}
	
	public default Outputs outputs() {
		return (Outputs)proxy("Outputs", Outputs.class);
	}

	public default NodeTemplates node_templates() {
		return (NodeTemplates)proxy("node_templates", NodeTemplates.class);
	}
	
	public default RelationshipTemplates relationship_templates() {
		return (RelationshipTemplates)proxy("relationship_templates", RelationshipTemplates.class);
	}

	public default Groups groups() {
		return (Groups)proxy("groups", Groups.class);
	}

	public default Substitution substitution_mappings() {
		return (Substitution)proxy("substitution_mappings", Substitution.class);
	}

}
