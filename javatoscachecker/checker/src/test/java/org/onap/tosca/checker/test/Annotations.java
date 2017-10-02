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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.annotations.Checks;


@Checks
public class Annotations {

	private Map<String,List> annons = new HashMap<String, List>();

	@Checks(path="/topology_template/node_templates")
	public void check_node_templates_annotations(Map theTemplates, Checker.CheckContext theContext) {
	
    for (Iterator<Map.Entry<String,Map>> i = theTemplates.entrySet().iterator(); i.hasNext(); ) {
  	    Map.Entry<String,Map> e = i.next();
				List nodeAnnons = (List)e.getValue().get("annotations");
				if (nodeAnnons != null)
					this.annons.put(e.getKey(), nodeAnnons);
		}
	}

	public boolean hasAnnotation(String thePath, String theValue) {

		List nodeAnnotations = annons.get(thePath);
		if (nodeAnnotations == null)
			return false;

		return nodeAnnotations.contains(theValue);
	}

}
