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

# This script installs java, logstash and the contrib package for logstash
# install java as prereq

apt-get update
apt-get install -y openjdk-7-jre-headless
mkdir /etc/logstash

# install by apt-get from repo
wget -O - http://packages.elasticsearch.org/GPG-KEY-elasticsearch | apt-key add -
echo "deb http://packages.elasticsearch.org/logstash/1.4/debian stable main" | tee -a /etc/apt/sources.list

apt-get update
apt-get install -y logstash

# install contrib to get the relp plugin
/opt/logstash/bin/plugin install contrib

# set up to run as service
update-rc.d logstash defaults 95 10
