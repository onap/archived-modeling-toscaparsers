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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.*; 

import java.util.Map;

import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.model.*;

/*
 * This is purely a navigational model, no processing, so we test that the data we expect can be found
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelTest {

	static Target						target;
  static ServiceTemplate	template;

	@BeforeClass
	public static void initialize() throws Exception {

		Checker checker = new Checker();
		checker.check(Tools.getTarget("models/full.yaml"));

		for (Target t: checker.catalog().targets()) {
			if (!t.getReport().isEmpty())
				System.out.println("Model checker issues:\n" + t.getReport());
		}

		target = checker.catalog().targets().iterator().next();
		template = Models.service_template(target);
	}

	@Test
	public void testServiceTemplate() {
	
		assertTrue("full service template".equals(template.description()));
	}
	
	@Test
	public void testServiceTemplateMetadata() {

		assertTrue(template.metadata() == null);
  }

	@Test
	public void testServiceTemplateDataTypes() {

		assertTrue(template.data_types() != null);

		for (DataType type: template.data_types().values()) {
      assertTrue(type.name().startsWith("data_type_"));
      assertTrue(type.description().startsWith("test data type"));
			assertTrue(type.version().equals("1.0"));

      Properties props = type.properties();
			assertTrue(props != null);

			for (Property prop: props.values()) {
      	assertTrue("property name prefix " + prop.name(), prop.name().startsWith("data_field_"));
				assertTrue("property description prefix " + prop.description(), prop.description().startsWith("test data field"));
				assertTrue(prop.required());

				String propType = prop.type();
      	assertTrue(propType != null && (propType.equals("string") || propType.equals("integer")));
			
				Constraints constraints = prop.constraints();
				if (constraints != null) {
					if ("integer".equals(propType))
						assertTrue("integer constraints", constraints.get(0).name().equals("valid_values"));
					if ("string".equals(propType))
						assertTrue("string constraints", constraints.get(0).name().equals("max_length"));
				}
			}
		}
	}

	@Test
	public void testServiceTemplateCapabilityTypes() {
		
		assertTrue(template.capability_types() != null);
		for (Map.Entry<String, CapabilityType> type: template.capability_types().entrySet()) {
      assertTrue(type.getKey().matches("capability_type_[0-9]"));
      
			Properties props = type.getValue().properties();
			assertTrue(props != null);

			for (Map.Entry<String, Property> prop: props.entrySet()) {
      	assertTrue(prop.getKey().matches("capability_type_[0-9]_property_[0-9]"));
			}
		}
	}
	
	@Test
	public void testServiceTemplateInterfaceTypes() {
		
		assertTrue(template.interface_types() != null);
		for (Map.Entry<String, InterfaceType> type: template.interface_types().entrySet()) {
      assertTrue(type.getKey(), type.getKey().matches("interface_type_[0-9]"));

			Operations ops = type.getValue().operations();
			if(ops != null) {
				for (Map.Entry<String, Operation> op: ops.entrySet()) {
  	    	assertTrue(op.getKey().matches("interface_type_[0-9]_op_[0-9]"));
				}
			}
		}
  }

	@Test
	public void testServiceTemplateRelationshipTypes() {
		
		assertTrue(template.relationship_types() != null);
		for (Map.Entry<String, RelationshipType> type: template.relationship_types().entrySet()) {
      assertTrue(type.getKey().matches("relationship_type_[0-9][0-9]"));

			TypeInterfaces infs = type.getValue().interfaces();
			assertTrue(infs != null);

			for (Map.Entry<String, TypeInterface> inf: infs.entrySet()) {
      	assertTrue(inf.getKey().matches("relationship_type_[0-9]+_interface_[0-9]+"));
			}
		}
	}
	
  @Test
	public void testServiceTemplateNodeTypes() {
		
    assertTrue(template.node_types() != null);
		for (NodeType nodeType: template.node_types().values()) {
      assertTrue(nodeType.name().matches("node_type_[0-9]"));

			Capabilities caps = nodeType.capabilities();
			assertTrue(caps != null);
			
			for (Capability cap: caps.values()) {
      	assertTrue(cap.name().matches("node_type_[0-9]_capability_[0-9]"));
			}

			Requirements reqs = nodeType.requirements();
			if (reqs != null) {
				for (Requirement req: reqs) {
					assertTrue(req.name().startsWith(nodeType.name() + "_requirement"));
					assertTrue(req.capability() != null);
					assertTrue(req.node() != null);
					assertTrue(req.relationship() != null);
				}
			}

			Artifacts arts = nodeType.artifacts();
			if (arts != null) {
				for (Artifact art: arts.values()) {
					assertTrue(art.name().startsWith(nodeType.name() + "_artifact"));
					assertTrue(art.type().startsWith("artifact_type"));
				}
			}

			TypeInterfaces itfs = nodeType.interfaces();
			if (itfs != null) {
				for (TypeInterface itf: itfs.values()) {
					assertTrue(itf.name().startsWith(nodeType.name() + "_interface"));
					assertTrue(itf.type().startsWith("interface_type"));
				}
			}

		}
	}
	
  @Test
	public void testServiceTemplateGroupTypes() {
		
    assertTrue(template.group_types() == null);
	}

  @Test
	public void testServiceTemplateTopologyTemplate() {

    assertTrue(template.topology_template() != null);
	}

  @Test
	public void testTopologyTemplateNodeTemplates() {

    assertTrue(template.topology_template().node_templates() != null);

		for (NodeTemplate node: template.topology_template().node_templates().values()) {
      assertTrue(node.name().matches("node_[0-9]"));

			PropertiesAssignments props = node.properties();
			assertTrue(props != null);

			for (Map.Entry<String, Object> prop: props.entrySet()) {
      	assertTrue(prop.getKey().matches("node_type_[0-9]_property_[0-9]"));
			}

			NodeType nodeType = template.node_types().get(node.type());
			if (nodeType.capabilities() != null) {
				assertTrue(node.capabilities() != null);
			}
		}
	}
  
	@Test
	public void testTopologyTemplateRelationshipTemplates() {
    
		assertTrue(template.topology_template().relationship_templates() != null);
	}

}

