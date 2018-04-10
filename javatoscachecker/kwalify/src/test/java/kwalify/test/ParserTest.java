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

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.io.InputStream;
import java.io.IOException;

import kwalify.PlainYamlParser;
import kwalify.SyntaxException;
import kwalify.Util;


/*
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ParserTest {

	@BeforeClass
	public static void initialize() throws Exception {
	}

	private Object parse(String theResource) throws SyntaxException {
		Object output = null;
		InputStream source = getClass().getClassLoader().getResourceAsStream(theResource);
		if (source == null) {
			fail("Cannot find test resource " + theResource);
		}

		try {
			output = new PlainYamlParser(
										Util.readInputStream(source))
									.parse();
		}
		catch (IOException iox) {
			fail("Failed to load test resource " + theResource + ": " + iox);
		}
		return output;
	}

	@Test
	public void testParserSuccess() {
		try {
			assertTrue(
				parse("parserSuccess.yml") instanceof Map);
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx);
		}
	}
	


	@Test
	public void testParserFailOnInvalidAnchor() {
		try {
			parse("parserFailInvalidAnchor.yml");
		}
		catch (SyntaxException sx) {
			assertTrue(sx.getMessage(), sx.getMessage().matches("anchor '.*' not found."));
			assertTrue("" + sx.getLineNumber(), sx.getLineNumber() == 25);
		}
	}
	
	@Test
	public void testParserFailIndentation() {
		try {
			parse("parserFailIndentation.yml");
		}
		catch (SyntaxException sx) {
			assertTrue(sx.getMessage(), sx.getMessage().matches("invalid indent of mapping."));
			assertTrue("" + sx.getLineNumber(), sx.getLineNumber() == 28);
		}
	}
	
	@Test
	public void testParserSeqOfScalar() {
		try {
			assertTrue(
				parse("parserSuccessSeqOfScalar.yml") instanceof List);
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
	}

	@Test
	public void testParserSeqOfMap() {
		Object res = null;
		try {
			res = parse("parserSuccessSeqOfMap.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue(res instanceof List);
		for (Object entry: (List)res)
			assertTrue(entry instanceof Map);
	}

	//@Test
	public void testParserSeqOfSeq() {
		Object res = null;
		try {
			res = parse("parserSuccessSeqOfSeq.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue(res instanceof List);
		for (Object entry: (List)res)
			assertTrue(entry instanceof List);
	}

	@Test
	public void testParserMapScalarToScalar() {
		Object res = null;
		try {
			res = parse("parserSuccessMapScalarToScalar.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue(res instanceof Map);
	}

	@Test
	public void testParserMapScalarToSeq() {
		Object res = null;
		try {
			res = parse("parserSuccessMapScalarToSeq.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue(res instanceof Map);
		for (Object value: ((Map)res).values())
			assertTrue(value instanceof List);
	}

	@Test
	public void testParserMapOfMap() {
		Object res = null;
		try {
			res = parse("parserSuccessMapOfMap.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		for (Object value: ((Map)res).values())
			assertTrue("value not a map, " + value.getClass() + ", " + value, value instanceof Map);
	}

	//@Test
	//not supported
	public void testParserMapOfSeqToSeq() {
		Object res = null;
		try {
			res = parse("parserSuccessMapOfSeqToSeq.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		for (Map.Entry entry: (Set<Map.Entry>)((Map)res).entrySet()) {
			assertTrue(entry.getKey() instanceof List);
			assertTrue(entry.getValue() instanceof List);
		}
	}

	//@Test
	//not supported
	public void testParserUnorderedSet() {
		Object res = null;
		try {
			res = parse("parserSuccessUnorderedSet.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a set: " + res.getClass() + ", " + res, res instanceof Set);
	}

	@Test
	public void testParserOrderedMap() {
		Object res = null;
		try {
			res = parse("parserSuccessOrderedMap.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a list: " + res.getClass() + ", " + res, res instanceof List);
	}

	@Test
	public void testParserFloats() {
		Object res = null;
		try {
			res = parse("parserSuccessFloats.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		
		for (Object value: ((Map)res).values())
			assertTrue("value not encoded as a Double: " + value.getClass() + ", " + value, value instanceof Double);
	}

	@Test
	public void testParserInts() {
		Object res = null;
		try {
			res = parse("parserSuccessInts.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		
		for (Object value: ((Map)res).values())
			assertTrue("value not encoded as a Integer: " + value.getClass() + ", " + value, value instanceof Integer);
	}

	@Test
	public void testParserQuotedStrings() {
		Object res = null;
		try {
			res = parse("parserSuccessQuotedStrings.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		
		for (Object value: ((Map)res).values())
			assertTrue("value not encoded as a String: " + value.getClass() + ", " + value, value instanceof String);
	}

	@Test
	public void testParserTimestamps() {
		Object res = null;
		try {
			res = parse("parserSuccessTimestamps.yml");
		}
		catch (SyntaxException sx) {
			fail("Unexpected syntax error " + sx + " at line " + sx.getLineNumber());
		}
		assertTrue("result not a map: " + res.getClass() + ", " + res, res instanceof Map);
		
		for (Object value: ((Map)res).values())
			assertTrue("value not encoded as a Date: " + value.getClass() + ", " + value, value instanceof java.util.Date);
	}

	@Test
	public void testMultipleDocs() throws SyntaxException {
		Object[] output = null;
		InputStream source = getClass().getClassLoader().getResourceAsStream("parserSuccessMultipleDocs.yml");
		if (source == null) {
			fail("Cannot find test resource parserSuccessMultipleDocs.yml");
		}

		try {
			output = new PlainYamlParser(
										Util.readInputStream(source))
									.parseAll();
		}
		catch (IOException iox) {
			fail("Failed to load test resource parserSuccessMultipleDocs.yml: " + iox);
		}
		assertTrue("Unexpected output " + output == null ? "null" : Arrays.toString(output), output != null && output.length == 2);
	}
}
