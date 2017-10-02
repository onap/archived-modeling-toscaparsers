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

import java.util.List;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.net.URI;
import java.net.URISyntaxException;

import org.onap.tosca.checker.Target;



/**
 * Credit for getResourceFiles goes to
 * 	https://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
 *
 * The spring framework contains classpath URL handler but I wanted to avoid bring all that in here.
 */
public class Tools {

	public static List<String> getResourceFiles(String thePath) {
    List<String> filenames = new ArrayList<>();

		try {
	    InputStream in = getResourceAsStream(thePath);
  	  BufferedReader br = new BufferedReader(new InputStreamReader(in));

			try {
  	    String resource;

				while( (resource = br.readLine()) != null ) {
  	      filenames.add( resource );
    	  }
    	}
			finally {
				try { br.close(); } catch (IOException iox) {}
			}
		}
		catch (IOException iox) {
			throw new UncheckedIOException(iox);
		}

    return filenames;
  }

  private static InputStream getResourceAsStream(String theResource) {
    final InputStream in
      = getContextClassLoader().getResourceAsStream(theResource);

    return in == null ? in.getClass().getResourceAsStream(theResource) : in;
  }

  private static ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

	public static Target getTarget(String theClassPath) {
		try {
			return new ClasspathTarget(theClassPath);
		}
		catch(URISyntaxException urix) {
			throw new RuntimeException(urix);
		}
	}

	/**
   * Simpler than handling classpath URIs
   */
	public static class ClasspathTarget extends Target {

		public ClasspathTarget(String theLocation) throws URISyntaxException {
			super(theLocation, new URI("classpath:" + theLocation));
		}

		@Override
		public Reader open() throws IOException {

    	return new BufferedReader(
						new InputStreamReader(
          		getResourceAsStream(getName())));
		}
	}


}
