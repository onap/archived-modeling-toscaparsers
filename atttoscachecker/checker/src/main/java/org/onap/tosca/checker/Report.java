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

import java.io.IOException;

import java.util.LinkedList;
import java.util.Collections;

import org.yaml.snakeyaml.error.MarkedYAMLException;
import kwalify.ValidationException;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Represents a collection of errors that occured during one of the stages
 * of the checker: yaml parsing, yaml validation (tosca syntax), tosca checking
 */
/*
 * This needs some re-thinking: while it is useful to have all original errors introducing
 * the custom json conversion (just to help the service) is not great either.
 * I was torn between this approach or creating a custom deserializer and object mapper (which
 * would have kept all the customized serialization in the service but then the error analysis
 * would be duplicated there too ..).  
 */
@JsonSerialize(contentUsing=org.onap.tosca.checker.Report.ReportEntrySerializer.class)
public class Report<T extends Throwable> extends LinkedList<T> {

	public Report() {
	}

	public Report(T[] theErrors) {
		Collections.addAll(this, theErrors);
	}

	public boolean hasErrors() {
		return !this.isEmpty();
	}

	public boolean addOnce(T theError) {
		for (T e: this) {
			if (e.getMessage().equals(theError.getMessage()))
				return false;
		}
		return add(theError);
	}

	public String toString() {
    StringBuilder sb = new StringBuilder(this.size() + " errors");
    for (Throwable x: this) {
      sb.append("\n")
				.append("[")
        .append(location(x))
        .append("] ")
				.append(x.getMessage());
				if (x.getCause() != null) {
					sb.append("\n\tCaused by:\n")
          	.append(x.getCause());
				}
    }
		sb.append("\n");
		return sb.toString();
	}

	private static String location(Throwable theError) {
		if (theError instanceof MarkedYAMLException) {
			MarkedYAMLException mx = (MarkedYAMLException)theError;
			return "line " + mx.getProblemMark().getLine() + ", column " + mx.getProblemMark().getColumn();
		}
		if (theError instanceof ValidationException) {
      ValidationException vx = (ValidationException)theError;
      return vx.getPath();
    }
		if (theError instanceof TargetError) {
    	TargetError tx = (TargetError)theError;
      return tx.getLocation();
		}
		return "unknown"; 
	}


	public static class ReportEntrySerializer extends StdSerializer<Throwable> {
		
		public ReportEntrySerializer() {
			super(Throwable.class);
		}

    @Override
    public void serialize(Throwable theError, JsonGenerator theGenerator, SerializerProvider theProvider) 
																												      throws IOException, JsonProcessingException {
      theGenerator.writeStartObject();
      theGenerator.writeStringField("location", location(theError));
      theGenerator.writeStringField("message", theError.getMessage());
			if (theError.getCause() != null)
      	theGenerator.writeStringField("cause", theError.getCause().toString());
      theGenerator.writeEndObject();
    }
	}
}

