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
package org.onap.tosca.checker.model;

import java.io.File;
import java.util.Map;

import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.Checker;


public class Models {

	public static ServiceTemplate service_template(Target theTarget) {
		return TOSCAProxy.buildObject("", (Map)theTarget.getTarget(), ServiceTemplate.class);
	}



  public static void main(String[] theArgs) {

		try {
			Catalog cat = new Checker().check(new File(theArgs[0]));

			for (Target t: cat.targets()) {
				System.err.println(t.getLocation() + "\n" + cat.importString(t) + "\n" + t.getReport());
			}

			//for (Target t: cat.sortedTargets()) {
			//	System.out.println(t);
			//}

			ServiceTemplate tmpl = service_template(cat.topTargets().iterator().next());

			NodeTypes node_types = tmpl.node_types();
			if (node_types != null) {
				for (NodeType nt: node_types.values()) {
					System.out.println(" ** node type : " + nt.name());
					Requirements reqs = nt.requirements();
					if (reqs != null) {
						for (Requirement req: reqs)
							System.out.println("\t ** requirement " + req.name() + "/" + req.capability());
					}
				}
			}

			Substitution subs = tmpl.topology_template().substitution_mappings();
			System.out.format("Substitution Mapping %s", subs.node_type());
			Substitution.Mappings maps = subs.capabilities();
			if (maps != null) {
				for (Substitution.Mapping m: maps.values())
					System.out.format("\n\tCapability mapping %s : %s)", m.target(), m.mapping());
			} 
			maps = subs.requirements();
			if (maps != null) {
				for (Substitution.Mapping m: maps.values())
					System.out.format("\n\tRequirement mapping %s : %s", m.target(), m.mapping());
			} 
/*
for(NodeTemplate nt: tmpl.topology_template().node_templates().values()) {
	System.out.println(" ** node : " + nt.name() + " : " + nt.type());
	PropertiesAssignments props = nt.properties();
	if (props != null) {
		props.entrySet().stream().forEach(e -> {
			System.out.println(" ** property : " + e.getKey() + " : " + e.getValue());
																						});
	}
*/
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
}

