#
# Copyright (c) 2017 <AT&T>.  All rights reserved.
# ===================================================================
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
# the specific language governing permissions and limitations under the License.
#
FROM openjdk:8

MAINTAINER "sj2381@att.com"

ADD Service-1.2.0-SNAPSHOT.jar /opt/modeling/Service-1.2.0-SNAPSHOT.jar
ADD application.properties /opt/modeling/application.properties
VOLUME /etc
ADD startService.sh /opt/modeling/startService.sh
RUN chmod 700 /opt/modeling/startService.sh

WORKDIR /opt/modeling/
EXPOSE 8080
ENTRYPOINT /opt/modeling/startService.sh


