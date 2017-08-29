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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a yaml document to be parsed/validated/checked.
 * A Target's scope is the checker that is processing it.
 */
public class Target {

	private static Logger log =
			Logger.getLogger("com.att.research.is.tosca.yaml.Target");

	private String 	name;
	private URI			location;
	//while it is convenient having the information below in here I am conflicted about it
	//as it represents processing state
	private Object 	target;								 //this is the parsed form of the target
	private Stage		stage = Stage.located; //the processinf stage this target has reached
	private	Report	report = new Report(); //collects the errors related to this target

	public Target(String theName, URI theLocation) {
		this.name = theName;
		this.location = theLocation;
	}

	public String getName() {
		return this.name;
	}

	public URI getLocation() {
		return this.location;
	}

	public Report getReport() {
		return this.report;
	}

	public void report(Throwable theError) {
		this.report.add(theError); 
	}
	
	public void report(String theErrMsg) {
		this.report.add(new Exception(theErrMsg)); 
	}

	public void setTarget(Object theTarget) {
		this.target = theTarget;
	}

	public Object getTarget() {
		return this.target;
	}

	public Stage getStage() {
		return this.stage;
	}

	public void setStage(Stage theStage) {
		if (theStage.ordinal() <= this.stage.ordinal())
			throw new IllegalArgumentException("Stage can only be set forward.");

		this.stage = theStage;
	}

	public void nextStage() {
		setStage(this.stage.next());
	}

	/*
	 * @return a reader for the source or null if failed
	 */
	public Reader open() throws IOException {

    return new BufferedReader(
						new InputStreamReader(
          		this.location.toURL().openStream()));
	}

	public String toString() {
		//return String.format("Target %s (%.20s ...)", this.location, this.target == null ? "" : this.target.toString());
		return String.format("Target %s at %s", this.name, this.location);

	}
}

