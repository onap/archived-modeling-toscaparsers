#!/bin/bash
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

# This script configures kibana to connect to the elasticsearch server
# to access data and to export the app url on port 5601:
# The environment variable elasticsearch_ip and kibana_ip are expected
# to be set up.
sed -i 's/localhost/'$elasticsearch_ip'/' /opt/kibana/config/kibana.yml
sed -i 's/0.0.0.0/'$kibana_ip'/' /opt/kibana/config/kibana.yml
