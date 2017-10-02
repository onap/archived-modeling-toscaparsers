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
package org.onap.tosca.checker.service.test;

import java.util.Scanner;

import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat; 

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import org.onap.tosca.checker.Report;

/**
 * The test order is relevant here ..
 */

//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.onap.tosca.checker.service.CheckerEngine.class,
								webEnvironment = WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckerServiceTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testCatalogExists() {
		assertTrue(
				this.restTemplate.getForEntity("/check_template/nosuchcatalog", String.class)
						.getStatusCodeValue() == 404);
	}

	@Test
	public void testStandaloneTemplate() {

		ResponseEntity<Report> response =
			this.restTemplate.exchange("/check_template/", HttpMethod.POST, prepareRequest("standalone.yaml"), Report.class);
		
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().size() == 0); //no errors
	}
	
	@Test
	public void testStandaloneTemplateWithErrors() {

		ResponseEntity<Report> response =
			this.restTemplate.exchange("/check_template/", HttpMethod.POST, prepareRequest("standalone_with_errors.yaml"), Report.class);
		
		assertTrue(response.getStatusCodeValue() == 200);
		assertTrue(response.getBody().size() > 0); //some errors
	}
	
	@Test
	public void testCatalog1WithNamedTemplate() {

		ResponseEntity<Report> response =
			this.restTemplate.exchange("/check_template/test/schema.yaml", HttpMethod.POST, prepareRequest("test_schema.yaml"), Report.class);
		
		assertTrue(response.getStatusCodeValue() == 200);
		assertThat(response.getBody().size() == 0)
					.as("Processing failed: " + response.getBody())
					.isTrue();
	}

	@Test
	public void testCatalog2WithTemplate() {
		
		ResponseEntity<Report> response =
			this.restTemplate.exchange("/check_template/test/", HttpMethod.POST, prepareRequest("test_template.yaml"), Report.class);
		
		assertTrue(response.getStatusCodeValue() == 200);
		assertThat(response.getBody().size() == 0)
					.as("Processing failed: " + response.getBody())
					.isTrue();
	}

	@Test
	public void testCatalog3NamedTemplateExists() {
		
		ResponseEntity<String> response =
			this.restTemplate.exchange("/check_template/test/schema.yaml", HttpMethod.GET, null, String.class);
		
		assertTrue(response.getStatusCodeValue() == 200);
	}

	@Test
	public void testCatalog4NamedTemplateDoesNotExists() {
		ResponseEntity<String> response =
			this.restTemplate.exchange("/check_template/test/test_schema.yaml", HttpMethod.GET, null, String.class);
		
		assertTrue(response.getStatusCodeValue() == 404);
	}
	
	@Test
	public void testCatalog5NamedTemplateDoesNotExists() {
		ResponseEntity<String> response =
			this.restTemplate.exchange("/check_template/test/test_schema.yaml", HttpMethod.GET, null, String.class);
		
		assertThat(response.getStatusCodeValue() == 404)
						.as("Existence check failed, got " + response.getStatusCodeValue())
						.isTrue();
	}
	
	@Test
	public void testCatalog6Delete() {
		ResponseEntity<String> response =
			this.restTemplate.exchange("/check_template/test/", HttpMethod.DELETE, null, String.class);
		
		assertThat(response.getStatusCodeValue() == 200)
						.as("Existence check failed, got " + response.getStatusCodeValue())
						.isTrue();
	}

	private HttpEntity prepareRequest(String theResourceName) {
		String content = new Scanner(
    									   Thread.currentThread().getContextClassLoader().getResourceAsStream(theResourceName), "UTF-8")
											.useDelimiter("\\Z").next();

		HttpHeaders headers = new HttpHeaders();
 		headers.setContentType(MediaType.APPLICATION_JSON);
 		return new HttpEntity<String>(content, headers);
	}
}
