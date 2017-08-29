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
package org.onap.tosca.checker.processing;

import java.util.function.Predicate;

import org.onap.tosca.checker.Report;

/**
 * 
 */
public interface Process<T extends Processor> {

	public static final int	PROCESS_SCOPE = 100;

	/**
   * the processor running this process
   */
	public T processor();

  /* */
	public boolean hasNext();

  /* */
	public Process runNext() throws ProcessorException;

	/* execute all steps to completion
   */
	public Report run();

	/* execution report
   */
	public Report report(); 

}
