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

/**
 */
public interface ServiceTemplate extends TOSCAObject<ServiceTemplate> {

	public String tosca_definitions_version();

	public String description();

	public default Metadata metadata() {
		return (Metadata)proxy("metadata", Metadata.class);
	}
	
	public default Imports imports() {
		return (Imports)proxy("imports", Imports.class);
	}
	
	public default Repositories repositories() {
		return (Repositories)proxy("repositories", Repositories.class);
	}
	
	public default ArtifactTypes artifact_types() {
		return (ArtifactTypes)proxy("artifact_types", ArtifactTypes.class);
	}
	
	public default DataTypes data_types() {
		return (DataTypes)proxy("data_types", DataTypes.class);
	}
	
	public default NodeTypes node_types() {
		return (NodeTypes)proxy("node_types", NodeTypes.class);
	}
	
	public default GroupTypes group_types() {
		return (GroupTypes)proxy("group_types", GroupTypes.class);
	}
	
	public default PolicyTypes policy_types() {
		return (PolicyTypes)proxy("policy_types", PolicyTypes.class);
	}
	
	public default RelationshipTypes relationship_types() {
		return (RelationshipTypes)proxy("relationship_types", RelationshipTypes.class);
	}
	
	public default CapabilityTypes capability_types() {
		return (CapabilityTypes)proxy("capability_types", CapabilityTypes.class);
	}
	
	public default InterfaceTypes interface_types() {
		return (InterfaceTypes)proxy("interface_types", InterfaceTypes.class);
	}

	public default TopologyTemplate topology_template() {
		return (TopologyTemplate)proxy("topology_template", TopologyTemplate.class);
	}
}
