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
 * Policy type definition, spec section 3.6.12
 */
public interface PolicyType extends TOSCAObject<PolicyType> {

	public String name();

	public String derived_from();
	
	public String description();
	
	public String version();
	
	public default Metadata metadata() {
		return (Metadata)proxy("metadata", Metadata.class);
	}
	
	public List<String> targets();

	public default Properties properties() {
		return (Properties)proxy("properties", Properties.class);
	}

	public default Triggers triggers() {
		return (Triggers)proxy("triggers", Triggers.class);
	}

	public interface Triggers extends TOSCAMap<Trigger> {

	}

	public interface Trigger extends TOSCAObject<Trigger> {

		public String description();
		
		public String event_type();
		
		public String schedule();
	
		public default Constraints constraint() {
			return (Constraints)proxy("constraint", Constraints.class);
		}

		public default Constraints condition() {
			return (Constraints)proxy("condition", Constraints.class);
		}

		public int period();
		
		public int evaluations();

		public String method();
		
		public String action();

		//target_filter	
		public default EventFilter target_filter() {
			return (EventFilter)proxy("target_filter", EventFilter.class);
		}

	}
	
	public interface EventFilter extends TOSCAObject<EventFilter> {

		public String node();

		public String requirement();

		public String capability();

	}

}
