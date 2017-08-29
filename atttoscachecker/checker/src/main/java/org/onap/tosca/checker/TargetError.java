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
package org.onap.tosca.checker;


/**
 * A target error represents an error in target the resource being checked.
 * We only represent it as a Throwable because the libraries that perform parsing and syntax validation
 * represent their errors as such ..
 */
public class TargetError extends Throwable {

	/*
	public static enum Level {
		error,
		warning	
	}
	*/

  private String		location; //we might need an more detailed representation
															//here: it could be a YAML document jpath or
															//document location (line).
  private String		target;

  public TargetError(String theTarget, String theLocation, String theMessage, Throwable theCause) {
		super(theMessage, theCause);
    this.target = theTarget;
		this.location = theLocation;
  }

  public TargetError(String theTarget, String theLocation, String theMessage) {
    this(theTarget, theLocation, theMessage, null);
  }

	public String getTarget() {
		return this.target;
	}

	public String getLocation() {
		return this.location;
	}


}

