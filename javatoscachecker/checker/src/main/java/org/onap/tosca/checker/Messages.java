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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/*
 * This class should be generated programatically based on the keys available in messages.properties
 */
public class Messages {

	private ResourceBundle	messages;
	
	public Messages() {
		try {
			this.messages = ResourceBundle.getBundle("org/onap/tosca/checker/messages");
		}
		catch (MissingResourceException mrx) {
			//default
			try {
				this.messages = ResourceBundle.getBundle("org/onap/tosca/checker/messages", new Locale("en", "US"));
			}
			catch (MissingResourceException mrxx) {
				throw new RuntimeException("Faile to load resources for locale or default resources", mrx);
			}
		}
		//check that the Message enum is in sync with the resource bundle
	}

	public String format(Message theMessage, Object[] theArgs) {
		String message = this.messages.getString(theMessage.name());
		if (message == null)
			throw new RuntimeException("Un-available message: " + theMessage);
 
		return MessageFormat.format(message, theArgs);
	}

	public enum Message {
		EMPTY_TEMPLATE,
		INVALID_CONSTRUCT_REFERENCE,
		INVALID_TYPE_REFERENCE,
		INVALID_TEMPLATE_REFERENCE,
		INVALID_INTERFACE_REFERENCE,
		INVALID_FACET_REFERENCE,
		INCOMPATIBLE_REQUIREMENT_TARGET
	}
}

