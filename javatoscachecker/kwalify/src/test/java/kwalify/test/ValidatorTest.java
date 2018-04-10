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
package kwalify.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.FixMethodOrder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.*; 

import java.util.Map;
import java.util.List;
import java.io.InputStream;
import java.io.IOException;

import kwalify.PlainYamlParser;
import kwalify.SyntaxException;
import kwalify.SchemaException;
import kwalify.Util;
import kwalify.Validator;


/*
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ValidatorTest {

	@BeforeClass
	public static void initialize() throws Exception {
	}

	protected Object[] prepareValidateTest(String theName) {
		Object[] res = new Object[3];
		InputStream source = null;

		source = getClass().getClassLoader().getResourceAsStream(theName + "Schema.yml");
		if (source == null) {
			fail("Cannot find test resource " + theName + "Schema.yml");
		}

		try {
			res[0] = new PlainYamlParser(
											Util.readInputStream(source))
									.parse();
		}
		catch (IOException iox) {
			fail("Failed to load test resource " + iox);
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx);
		}

		source = getClass().getClassLoader().getResourceAsStream(theName + ".yml");
		if (source == null) {
			fail("Cannot find test resource " + theName + ".yml");
		}
		try {
			res[1] = new PlainYamlParser(
											Util.readInputStream(source))
									.parse();
		}
		catch (IOException iox) {
			fail("Failed to load test resource " + iox);
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx);
		}

		source = getClass().getClassLoader().getResourceAsStream("in" + theName + ".yml");
		if (source == null) {
			fail("Cannot find test resource in" + theName + ".yml");
		}
		try {
			res[2] = new PlainYamlParser(
											Util.readInputStream(source))
									.parse();
		}
		catch (IOException iox) {
			fail("Failed to load test resource " + iox);
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx);
		}

		return res;
	}

	protected void runValidateTest(String theTestName) {
		Object[] docs = prepareValidateTest(theTestName);
		List errors = null;
		try {
			errors = new Validator(docs[0]).validate(docs[1]);
		}
		catch (SchemaException sx) {
			fail("Invalid schema " + sx);
		}

		assertTrue(errors.toString(), errors != null && errors.size() == 0);		
	
		try {
			errors = new Validator(docs[0]).validate(docs[2]);
		}
		catch (SchemaException sx) {
			fail("Invalid schema " + sx);
		}

		assertTrue(errors.toString(), errors != null && errors.size() > 0);		
	}
	
	@Test
	public void testMap() {
		runValidateTest("validateMap");
	}

	@Test
	public void testSeq() {
		runValidateTest("validateSeq");
	}

	@Test
	public void testMapOfSeq() {
		runValidateTest("validateMapOfSeq");
	}
	
	@Test
	public void testSeqOfMap() {
		runValidateTest("validateSeqOfMap");
	}
	
	@Test
	public void testRule() {
		runValidateTest("validateRule");
	}
}
