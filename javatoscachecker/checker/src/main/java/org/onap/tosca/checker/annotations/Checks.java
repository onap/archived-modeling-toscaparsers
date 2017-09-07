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
package org.onap.tosca.checker.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * Marks a method as a processing step during the checking phase. Allows for additional checking to be 'plugged' in.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD}) 
/**
 * Marks a method as participating in the consistency checking stage. The method is expected to have the following
 * signature:
 *	param theDef type of expected representation of the construct's information, for example Map<String,Map> for
 * 			node_types or List<Map> for requirements
 *  param theContext	CheckContext instance exposing the checker state
 * 
 * Note: currently not used at type level
 */
/* The iffy part: as a type annotaton we do not need a path or a version specification,
   as a method annotation it is mandatory (cannot be the default) 
	 We could forsee that a version indication at type level would cover all check handler within the type
 */
public @interface Checks {
  String path() default "/";
	String[] version() default { "tosca_simple_yaml_1_0", "tosca_simple_yaml_1_0_0",
															 "tosca_simple_yaml_1_1", "tosca_simple_yaml_1_1_0" };
}
