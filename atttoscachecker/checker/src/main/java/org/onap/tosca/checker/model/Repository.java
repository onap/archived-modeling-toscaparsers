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


/*
 * Repository definition (spec section 3.6.3)
 */
public interface Repository extends TOSCAObject<Repository> {

	/** */
	public String name();

	/** */
	public String description();

	/** */
	public String url();
	
	/** */
	public default Credential credential() {
		return (Credential)proxy("credential", Credential.class);
	}

	/** */
	public interface Credential extends TOSCAObject<Credential> {
	
		/** */
		public String protocol();
		
		/** */
		public String token_type();
		
		/** */
		public String token();
		
		/** */
		public String user();
		
		/** */
		public Map<String,String> keys();
	}
}
