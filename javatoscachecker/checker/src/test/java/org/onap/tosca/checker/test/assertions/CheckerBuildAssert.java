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
package org.onap.tosca.checker.test.assertions;

import org.assertj.core.api.AbstractAssert;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.CheckerException;

/**
 */
public class CheckerBuildAssert extends AbstractAssert<CheckerBuildAssert, Checker.CheckerConfiguration> {

	protected CheckerBuildAssert(Checker.CheckerConfiguration theConfig) {
		super(theConfig, CheckerBuildAssert.class);
	}

	public CheckerAssert builds() {
		try {
			return new CheckerAssert(this.actual == null ? new Checker() : new Checker(this.actual));
		}
		catch (CheckerException cx) {
			failWithMessage("Failed to build checker with given configuration: %s", cx.getMessage());
			return null; //we should not make it here
		}
	}

}
