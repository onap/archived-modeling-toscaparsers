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

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Paths;

import java.util.Set;
import java.util.LinkedHashSet;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Iterables;


public class CommonLocator implements TargetLocator {
	
	private static Logger log =
		Logger.getLogger("com.att.research.is.tosca.yaml.TargetLocator");

	private Set<URI> searchPaths = new LinkedHashSet(); 

	/* will create a locator with 2 default search paths: the file directory 
	 * from where the app was and the jar from which this checker (actually this
	 * class) was loaded */
	public CommonLocator() {
		addSearchPath(
			Paths.get(".").toAbsolutePath().normalize().toUri());
	}
	
	public CommonLocator(String... theSearchPaths) {
		for (String path: theSearchPaths) {
			addSearchPath(path);
		}
	}

	public boolean addSearchPath(URI theURI) {

		if (!theURI.isAbsolute()) {
			log.log(Level.WARNING, "Search paths must be absolute uris: " + theURI);
			return false;
		}

		return searchPaths.add(theURI);
	}

	public boolean addSearchPath(String thePath) {
		URI suri = null; 
		try {
			suri = new URI(thePath);
		}
		catch(URISyntaxException urisx) {
			log.log(Level.WARNING, "Invalid search path: " + thePath, urisx);
			return false;
		}

		return addSearchPath(suri);
	}

	public Iterable<URI> searchPaths() {
		return Iterables.unmodifiableIterable(this.searchPaths);
	}

	/**
	 * Takes the given path and first URI resolves it and then attempts to open
	 * it (a way of verifying its existence) against each search path and stops
	 * at the first succesful test.
	 */
	public Target resolve(String theName) {
		URI puri = null;
		InputStream	pis = null;

		//try absolute
		try {
			puri = new URI(theName);
			if (puri.isAbsolute()) {
				try {
					pis = puri.toURL().openStream();
				}
				catch (IOException iox) {
					log.log(Level.WARNING, "The path " + theName + " is an absolute uri but it canont be opened", iox);
					return null;
				}finally {
	                if (pis!= null) {
	                    try {
	                        pis.close();
	                    }
	                    catch (IOException iox) {
	                        log.log(Level.FINER,"Error while closing input stream",iox);
	                    }
	                }
	            }
			}
		}
		catch(URISyntaxException urisx) {
			log.log(Level.FINER, "TargetResolver failed attempting " + puri, urisx);
			//keep it silent but what are the chances ..
		}

		//try relative to the search paths
		for (URI suri: searchPaths) {
			try {
				puri = suri.resolve(theName);
				log.finer("TargetResolver trying " + puri);
				pis = puri.toURL().openStream();
				return new Target(theName, puri.normalize());
			}
			catch (Exception x) {
				log.log(Level.FINER, "TargetResolver failed attempting " + puri, x);
				continue;
			}
			finally {
				if (pis!= null) {
					try {
						pis.close();
					}
					catch (IOException iox) {
					    log.log(Level.FINER,"Error while closing input stream",iox);
					}
				}
			}
		}

		//try classpath
		URL purl = getClass().getClassLoader().getResource(theName);
		if (purl != null) {
			try {
				return new Target(theName, purl.toURI());
			}
			catch (URISyntaxException urisx) {
			    log.log(Level.FINER,"Error while creating Target",urisx);
			}
		}

		return null;
	}

	public String toString() {
		return "CommonLocator(" + this.searchPaths + ")";
	}

	
	public static void main(String[] theArgs) {
		TargetLocator tl = new CommonLocator();
		tl.addSearchPath(java.nio.file.Paths.get("").toUri());
		tl.addSearchPath("file:///");
		System.out.println(
				tl.resolve(theArgs[0]));
	}
}
