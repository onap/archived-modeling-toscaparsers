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

This CSAR contains all definitions that are required for deploying a simple
vRNC(virtual Radio Network Controller) on a cloud.

Entry information for processing through an orchestrator is contained in file
TOSCA-Metadata/TOSCA.meta. This file provides high-level information such as
CSAR version or creator of the CSAR. Furthermore, it provides pointers to the
various TOSCA definitions files that contain the real details.
The entry 'Entry-Definitions' points to the definitions file which holds the
service template for the workload.
'Entry-Definitions' is optional. An orchestrator can also process the contents
like this:
1) Read in and process each definitions file.
2) For each definitions file:
  2.1) Read in all * type definitions (node types, capability types, etc.) and
       store them in an internal map
3) Verify and build dependencies (e.g. inheritance) between all type definitions
   previously read in. Orchestrator built-in types (e.g. TOSCA base types) are
   also considered in this step.
4) Process the actual service template (the file with a node_templates section).
   Validate using previously obtained type information.