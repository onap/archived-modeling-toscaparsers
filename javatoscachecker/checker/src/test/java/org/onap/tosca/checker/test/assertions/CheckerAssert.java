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

import java.util.Map;

import java.util.function.Predicate;

import org.assertj.core.api.AbstractAssert;
import static org.assertj.core.api.Assertions.*; 

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.Report;
import org.onap.tosca.checker.Catalog;

/**
 */
public class CheckerAssert extends AbstractAssert<CheckerAssert, Checker> {

	protected CheckerAssert(Checker theActualChecker) {
		super(theActualChecker, CheckerAssert.class);
	}

	public CheckerAssert checks(String theTarget) {
		assertThatCode(() -> {
						this.actual.check(theTarget);
					}).doesNotThrowAnyException();

		return this;
	}
	
	public CheckerAssert checks(Target theTarget) {
		assertThatCode(() -> {
						this.actual.check(theTarget);
					}).doesNotThrowAnyException();

		return this;
	}

	public CheckerAssert withNoErrors() {
		Catalog cat = this.actual.catalog();
		for (Target t: cat.targets()) {
			assertThat(t.getReport().isEmpty())
						.as("No errors were expected for " + t + "\n" + t.getReport())
						.isTrue();
		}

		return this;
	}

	public CheckerAssert withExpectedOutcome() {
		Catalog cat = this.actual.catalog();
		for (Target t: cat.targets()) {
			
			//no content means that parsing might have failed ..
			Map content = (Map)t.getTarget();
			assertThat(content != null)
				.as("Parsing failed for " + t + "\n" + t.getReport())
				.isTrue();

			String outcome = "success";
			Map metadata = (Map)content.get("metadata");
			//assertThat(metadata != null)
			//	.as("No metadata was defined in " + t)
			//	.isTrue();
			if (metadata != null) {
				outcome = (String)metadata.get("outcome");
			}
			//assertThat(outcome != null)
			//	.as("No expected outcome was defined in " + t)
			//	.isTrue();

			if ("fail".equals(outcome))
				assertThat(t.getReport().isEmpty())
					.as("Checking was expected to fail but succeeded for " + t)
					.isFalse(); //should go further and make sure teh expected error was generated
			else if ("success".equals(outcome))
				assertThat(t.getReport().isEmpty())
					.as("Checking was expected to succeed but failed for " + t + "\n" + t.getReport())
					.isTrue(); //should go further and make sure teh expected error was generated
		}

		return this;
	}	

	public CheckerAssert withTruePredicate(Predicate<Checker> thePredicate) {
		assertThat(thePredicate.test(this.actual))
						.isTrue();
		return this;
	}
}
