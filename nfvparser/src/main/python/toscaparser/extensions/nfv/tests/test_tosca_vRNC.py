#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.

import os

from toscaparser.tests.base import TestCase
from toscaparser.tosca_template import ToscaTemplate


class ToscaVRNCTemplateTest(TestCase):

    '''NFV TOSCA vRNC template.'''
    tosca_tpl = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "data/vRNC/Definitions/vRNC.yaml")
    tosca = ToscaTemplate(tosca_tpl)

    def test_version(self):
        self.assertEqual(self.tosca.version,
                         "tosca_simple_profile_for_nfv_1_0_0")

    def test_input(self):
        input_names = sorted(["mm_storage_size", "id",
                              "vendor", "version"])
        self.assertEqual(sorted([i.name for i in self.tosca.inputs]),
                         input_names)

    def test_nodetemplates(self):
        expected_node_list = sorted(
            ["MM_Active", "MM_Passive", "MM_BlockStorage",
             "MM_Active_Host", "MM_Passive_Host",
             "CM_Active", "CM_Passive", "DM", "LB",
             "CM_Active_Host", "CM_Passive_Host", "DM_Host", "LB_Host",
             "EXTERMEDIA_Net", "INTERMEDIA_Net", "EMS_Net", "CTRL_Net",
             "MM_Active_Port_EMS", "MM_Active_Port_CTRL",
             "MM_Active_Port_EXTERMEDIA", "MM_Active_Port_INTERMEDIA",
             "MM_Passive_Port_EMS", "MM_Passive_Port_CTRL",
             "MM_Passive_Port_EXTERMEDIA", "MM_Passive_Port_INTERMEDIA",
             "CM_Active_Port_CTRL", "CM_Active_Port_INTERMEDIA",
             "CM_Passive_Port_CTRL", "CM_Passive_Port_INTERMEDIA",
             "DM_Port_CTRL", "DM_Port_INTERMEDIA",
             "LB_Port_INTERMEDIA", "LB_Port_EXTERMEDIA", "LB_Port_CTRL"])

        node_list = sorted([node.name for node in self.tosca.nodetemplates])
        self.assertEqual(node_list, expected_node_list)

    def test_output(self):
        expected_output_name = sorted(
            ["private_ip_of_CM", "private_ip_of_DM",
             "private_ip_of_LB", "private_ip_of_MM"])

        output_list = sorted([output.name for output in self.tosca.outputs])
        self.assertEqual(output_list, expected_output_name)
