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

# Edit the file /etc/mongod.conf, update with real IP of Mongo server
# This script configures the mongodb server to export its service on
# the server IP
# bind_ip = 127.0.0.1     -> bind_ip = <IP for Mongo server>
# The environment variable mongodb_ip is expected to be set up
sed -i "s/= 127.0.0.1/= $mongodb_ip,127.0.0.1/" /etc/mongod.conf
