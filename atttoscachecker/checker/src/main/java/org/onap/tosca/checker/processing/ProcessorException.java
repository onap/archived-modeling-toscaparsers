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
package org.onap.tosca.checker.processing;

import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.CheckerException;


/**
 */
public class ProcessorException extends CheckerException {

	private Target	target;

  public ProcessorException(Target theTarget, String theMsg, Throwable theCause) {
    super(theMsg, theCause);
		this.target = theTarget;
  }

  public ProcessorException(Target theTarget, String theMsg) {
    super(theMsg);
		this.target = theTarget;
  }

	public Target getTarget() {
		return this.target;
	}

	@Override
	public String getMessage() {
		return this.target + ":" + super.getMessage() + (getCause() == null ? "" : ("(" + getCause() + ")"));
	}
}
