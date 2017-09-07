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
 * Spec section 3.5.12
 */
public interface Parameter extends TOSCAObject<Parameter> {

	public String name();

	public String type();

	public String description();
	
	public Object value();

	public default Object _default() {
		return info().get("default");
	}

	public boolean required();

	public Status status();

	public default Constraints constraints() {
		return (Constraints)proxy("constraints", Constraints.class);
	}

	public default EntrySchema entry_schema() {
		return (EntrySchema)proxy("entry_schema", EntrySchema.class);
	}

}