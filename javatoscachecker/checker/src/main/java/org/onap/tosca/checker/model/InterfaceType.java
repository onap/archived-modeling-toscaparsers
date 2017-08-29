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

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Interface type definition, spec section 3.6.5
 */
public interface InterfaceType extends TOSCAObject<InterfaceType> {

	public String name();

	public String derived_from();
	
	public String description();
	
	public String version();
	
	public default Metadata metadata() {
		return (Metadata)proxy("metadata", Metadata.class);
	}

	public default Properties inputs() {
		return (Properties)proxy("inputs", Properties.class);
	}

	/**
   * The set of operations, made up by all keys but the ones above ..
   */
	public default Operations operations() {
		return (Operations)
			TOSCAProxy.record(info(),
												info ->	TOSCAProxy.buildMap(null,
																										Maps.filterKeys((Map)info,
													 																					key -> !("derived_from".equals(key) ||
																											  						 				 "description".equals(key) ||
																																						 "version".equals(key) ||
																																						 "metadata".equals(key) ||
																																						 "inputs".equals(key))),
															 											Operations.class));
	
	}
	

}
