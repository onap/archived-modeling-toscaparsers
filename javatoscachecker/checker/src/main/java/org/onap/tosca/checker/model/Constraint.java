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
 * constraint definition, spec section 3.6.6
 */
public interface Constraint extends TOSCAObject<Constraint> {

	public String name();

	public default Constraint.Type type() {
		return Type.valueOf(Type.class, name());
	}

	/* this is a one entry map so here we pick the single 
	 */
	public default Object expression() {
		return info().values().iterator().next();
	}

	public enum Type {
		equal,
		greater_than,
		greater_or_equal,
		less_than,
		less_or_equal,
		in_range,
		valid_values,
		length,
		min_length,
		max_length,
		pattern
	}
}
