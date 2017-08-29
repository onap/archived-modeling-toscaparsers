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
import java.util.Map;

import com.google.common.collect.Maps;


/*
 * Interface definition used in templates (node, relationship)
 */
public interface TemplateInterface extends TOSCAObject<TemplateInterface> {

	public String name();

	public default Inputs inputs() {
		return (Inputs)proxy("inputs", Inputs.class);
	}

	/**
	 * See InterfaceType for the reason for the implementation below. 
	 * Use the template specific operation definition, as per spec section 3.5.13.2.3
	 */
	public default TemplateInterface.Operations operations() {
		return (Operations)
			TOSCAProxy.record(info(),
												info ->	TOSCAProxy.buildMap(null,
																										Maps.filterKeys((Map)info,
													 																					key -> !("inputs".equals(key))),
															 											Operations.class));
	}

	/**
	 * Is this to be viewed as an 'operation assignment' ??
	 */	
	public interface Operations extends TOSCAMap<Operation> {
	}

	/*
	 * Template specific operation definition, section 3.5.13.2.3
	 */
	public interface Operation extends TOSCAObject<Operation> {

		public String name();
		
		public String description();
	
		public default PropertiesAssignments inputs() {
			return (PropertiesAssignments)proxy("inputs", PropertiesAssignments.class);
		}
		
		public default Implementation implementation() {
			return (Implementation)proxy("implementation", Implementation.class);
		}

		/**
		 */
		public interface Implementation extends TOSCAObject<Implementation> {
		
			public String primary();
			
			public List<String> dependencies();
	
		}
			
	}

}
