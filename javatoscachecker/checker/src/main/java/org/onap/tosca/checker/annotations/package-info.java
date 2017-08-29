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

/**
 * Contains the definition of the annotations used to mark methods used in processing a TOSCA template, one annotation
 * for each stage (not for locate and parsed: the location process is done through org.onap.tosca.checker.Locator and 
 * parsing is currently built in).
 *
 * The checker {@link org.onap.tosca.checker.Checker Checker} scans the classpath for classes with methods annotated 
 * accordingly to annotations in this package (currently we scan packages prefixed with org.onap.tosca).
 * 
 * At different stages the checker will delegate processing to this annotated methods. 
 *
 * {@link org.onap.tosca.checker.annotations.Validates Validates} marks a method as part of the validation stage, i.e.
 * syntax checking. It requires the indication of the grammar rule it intends to add validation to and wether it
 * performs pre or post processing.  
 *
 * {@link org.onap.tosca.checker.annotations.Checks Checks} marks a method as part of the consistency checks stage.
 * A method such annotated will be invoked when the processing reaches the document path indicated in the annotation.
 * A document path follows the strcture of the TOSCA yaml document, for example:
 *		/node_types/some_node_type
 *		/topology_template/node_templates
 * Note that current implementation works by delegation down the hierachical structure of the document. If no processor
 * is in place for /topology_templates/ or the one(s) in place do not delegate any further then a processor for
 * /topology_template/node_templates will not get involved. The framework only attempts the invocation of first level
 * (hierachically speaking) checks.
 *
 * {@link org.onap.tosca.checker.annotations.Catalogs Catalogs} marks a method as part of the cataloging stage, i.e.
 * the storage/indexing (of some sort, it does not really need to be storing) of a construct that has been validated.
 * Same as with {@link org.onap.tosca.checker.annotations.Checks Checks} the scope of a Cataloger is determined through
 * a document path specification.
 *
 * A third party can provide its own processing (by using these annotations) and perform custom verifications, indexing
 * for special post-processing, etc. 
 */

package org.onap.tosca.checker.annotations;
