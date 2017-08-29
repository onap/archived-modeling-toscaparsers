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

/*
 * Interface definition used in types (node, relationship)
 */
public interface TypeInterface extends TOSCAObject<TypeInterface> {

	public String name();
	
	public String type();

	public default Inputs inputs() {
		return (Inputs)proxy("inputs", Inputs.class);
	}

	public default Operations operations() {
		//not as obvious because we need to pick up all the keys EXCEPT "inputs" and "type";
		//as a result we cannot pick a key to attach the proxy to (and end up in the best case creating one every time ..).
		//I wish I had a more elegant solution but in order to avoid building a new proxy every time we get here
		//we register the long-lasting reference to proxy association ..

		return (Operations)
			TOSCAProxy.record(info(),
												info ->	TOSCAProxy.buildMap(null,
																										Maps.filterKeys((Map)info,
													 																					key -> !("type".equals(key) ||
																											  						 				 "inputs".equals(key))),
															 											Operations.class));
	}

}
