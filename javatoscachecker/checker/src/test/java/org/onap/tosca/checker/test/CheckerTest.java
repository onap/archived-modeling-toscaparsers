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
package org.onap.tosca.checker.test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*; 

import static org.onap.tosca.checker.test.assertions.CheckerAssertions.*;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.Target;

/**
 * Basic tests of Checker
 */
public class CheckerTest {



	@Test
	public void testCreateChecker() {
		assertThatCode(() -> {
											new Checker();
									}).doesNotThrowAnyException();
	}

	
	@Test
	public void testCreateCheckerWithEmptyConfiguraton() {
		assertThatCode(() -> {
											new Checker(new Checker.CheckerConfiguration());
									}).doesNotThrowAnyException();
	}
	
	@Test
	public void testCreateCheckerWithConfiguratonWithGrammar() {
		assertThatCode(() -> {
											new Checker(new Checker.CheckerConfiguration()
																				.withGrammars("config/tosca_simple_yaml_1_1_extended.grammar"));
									}).doesNotThrowAnyException();
	}

	@Test
	public void testCreateCheckerWithConfiguratonWithPaths() {
		assertThatCode(() -> {
											new Checker(new Checker.CheckerConfiguration()
																				.withDefaultImportsPath("config/"));
									}).doesNotThrowAnyException();
	}

	/*
	 * this is the positive test
	 */
	@Test
	public void testCheckGrammarExtension() {

		assertThat(new Checker.CheckerConfiguration()
										.withGrammars("config/tosca_simple_yaml_1_1_extended.grammar"))
						.builds()
						.checks(Tools.getTarget("config/annotations.yaml"))
						.withNoErrors();
	}

	/** test that provided extensions get called at every relevant processing stage */
	@Test
	public void testCheckExtension() {

		assertThat(new Checker.CheckerConfiguration()
										.withGrammars("config/tosca_simple_yaml_1_1_extended.grammar"))
						.builds()
						.checks(Tools.getTarget("config/annotations.yaml"))
						.withNoErrors()
						.withTruePredicate(checker -> checker.getHandler(Annotations.class).hasAnnotation("test", "this"));
	}	


  @Test
  public void testCheckDataTypes() {
		testCheckInputFiles("spec/data_types");
	}
  
	@Test
  public void testCheckCapabilityTypes() {
		testCheckInputFiles("spec/capability_types");
	}
  
	@Test
  public void testCheckNodeTypes() {
		testCheckInputFiles("spec/node_types");
	}
	
	@Test
  public void testCheckTopologyTemplate() {
		testCheckInputFiles("spec/topology_template");
	}
	
	@Test
  public void testCheckExamples() {
		testCheckInputFiles("spec/examples");
	}

	private void testCheckInputFiles(final String theLocation) {

		//creates a checker for each test file, is that really necessary?
		for (String filename: Tools.getResourceFiles(theLocation)) {
			assertThat()
				.builds()
				.checks(Tools.getTarget(theLocation + "/" + filename))
				.withExpectedOutcome();
		}
	}

}

