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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import org.onap.tosca.checker.annotations.Catalogs;

/*
 * Facilitates processing of catalog information through xpath expressions.
 * In development.
 */
public class Paths {

	private Map<Target, JXPathContext>	paths = new HashMap<Target, JXPathContext>();
	private JXPathContext								types = JXPathContext.newContext(new HashMap());

	@Catalogs(path="/")
	public void catalog_root(Map theDefinition, Checker.CheckContext theContext) {
		paths.put(theContext.target(), JXPathContext.newContext(theContext.target().getTarget()));
		theDefinition
			.forEach((k,v) -> { if (k.toString().endsWith("_types"))
														((Map)types.getContextBean()).merge(k, v, (v1, v2) -> { ((Map)v1).putAll((Map)v2);
																																										return v1; 
																																									});
												});
	}
	
	public Object resolve(Target theTarget, String thePath) {
		return paths.get(theTarget).getValue(thePath);
	}
	
	public Object resolve(String thePath) {
		return types.getValue(thePath);
	}

	/* */
	public String nodeType(String theNodeType) {
		return (String)resolve("/node_types/" + theNodeType);
	}

	public Map nodeTypeInterface(String theNodeType, String theInterfaceName) {
		return (Map)resolve("/node_types/" + theNodeType + "/interfaces/" + theInterfaceName);
	}
	
	public String nodeTypeInterfaceType(String theNodeType, String theInterfaceName) {
		return (String)resolve("/node_types/" + theNodeType + "/interfaces/" + theInterfaceName + "/type");
	}

	/* */
	public Map interfaceType(String theInterfaceType) {
		return (Map)resolve("/interface_types/" + theInterfaceType);
	}

	//this will also resolve 'metadata', 'derived_from', version and other common entries as operations
	public Map interfaceTypeOperation(String theInterfaceType, String theOperationName) {
		return (Map)resolve("/interface_types/" + theInterfaceType + "/" + theOperationName);
	}

	/* node templates */
	public String nodeTemplateType(Target theTarget, String theNodeName) {
		return (String)resolve(theTarget, "/topology_template/node_templates/" + theNodeName + "/type");
	}

	public Map nodeTemplateInterface(Target theTarget, String theNodeName, String theInterfaceName) {
		return (Map)resolve(theTarget, "/topology_template/node_templates/" + theNodeName + "/interfaces/" + theInterfaceName);
	}

	public Map nodeTemplateInterfaceOperation(
								Target theTarget, String theNodeName, String theInterfaceName, String theOperationName) {
		return (Map)resolve(theTarget, "/topology_template/node_templates/" + theNodeName + "/interfaces/" + theInterfaceName + "/" + theOperationName);
	}


	/* */
	public Map nodeTypeInterfaceOperation(String theNodeType, String theInterfaceName, String theOperationName) {
		return (Map)resolve("/node_types/" + theNodeType + "/interfaces/" + theInterfaceName + "/" + theOperationName);
	}
	
	
}
