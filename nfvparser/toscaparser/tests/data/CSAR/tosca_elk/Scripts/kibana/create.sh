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

# This script installs kibana and sets it up to run as a service in init.d
cd /opt
wget https://download.elastic.co/kibana/kibana/kibana-4.1.0-linux-x64.tar.gz
tar xzvf kibana-4.1.0-linux-x64.tar.gz
mv kibana-4.1.0-linux-x64 kibana

# set up to run as service
cd /etc/init.d
wget https://gist.githubusercontent.com/thisismitch/8b15ac909aed214ad04a/raw/bce61d85643c2dcdfbc2728c55a41dab444dca20/kibana4
chmod +x kibana4
update-rc.d kibana4 defaults 96 9
