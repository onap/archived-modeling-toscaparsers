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

import java.util.List;

/**
 * Relationship type definition, spec section 3.6.10
 */
public interface RelationshipType extends TOSCAObject<RelationshipType> {

	public String name();

	public String derived_from();
	
	public String description();
	
	public String version();
	
	public default Metadata metadata() {
		return (Metadata)proxy("metadata", Metadata.class);
	}

	public default Properties properties() {
		return (Properties)proxy("properties", Properties.class);
	}

	public default Attributes attributes() {
		return (Attributes)proxy("attributes", Attributes.class);
	}

	public default TypeInterfaces interfaces() {
		return (TypeInterfaces)proxy("interfaces", TypeInterfaces.class);
	}

	public List<String> valid_target_types();
	
}
