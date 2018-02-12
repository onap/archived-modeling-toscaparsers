
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

README:

This TOSCA simple profile deploys nodejs, mongodb, elasticsearch, logstash and kibana each on a separate server with monitoring enabled for nodejs server where a sample nodejs application is running. The syslog and collectd are installed on a nodejs server.

Entry information for processing through an orchestrator is contained in file TOSCA-Metadata/TOSCA.meta. This file provides high-level information such as CSAR version or creator of the CSAR. Furthermore, it provides pointers to the entry template under 'Entry-Definitions' key. The entry template itself may contain pointers to one or more files that are used to define TOSCA base type, unless provided by orchestrator as built-in TOSCA basetypes, and other non-normative types. These are typically provided under 'imports' section in the entry template file. Those type definitions will be read and processed by orchestrator or TOSCA parser to create an internal graph showing dependencies and relationships between various TOSCA types. The entry template may have references to various artifacts required for deployment and will be processed accordingly.
