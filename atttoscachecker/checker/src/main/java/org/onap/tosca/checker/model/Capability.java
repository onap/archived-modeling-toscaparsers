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

/*
 * Type specific capability definition, spec section 3.6.2
 */
public interface Capability extends TOSCAObject<Capability> {

	/**
	 * The required name of the Capability Type the capability definition is based upon.
   */
	public String type();

	/**
	 * The optional description of the Capability definition.
	 */	
	public String description();

	/**
	 * An optional list of one or more valid names of Node Types that are supported as valid sources of any 
	 * relationship established to the declared Capability Type.
	 */
	public List<String> valid_source_types();

	/**
	 * The optional minimum and maximum occurrences for the capability.
	 * By default, an exported Capability should allow at least one relationship to be formed with it with a
	 * maximum of UNBOUNDED relationships.
	 */
	public default Range occurences() {
		return (Range)proxyList("occurences", Range.class);
	}

	/**
	 * An optional list of property definitions for the Capability definition.
	 */
	public default Properties properties() {
		return (Properties)proxy("properties", Properties.class);
	}

	/**
	 * An optional list of attribute definitions for the Capability definition.
	 */
	public default Attributes attributes() {
		return (Attributes)proxy("attributes", Attributes.class);
	}

}
