#!/bin/sh
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

sed --regexp-extended "s/(port\s*=\s*)[0-9]*/\1$db_port/g" </etc/mysql/my.cnf >/tmp/my.cnf
mv -f /tmp/my.cnf /etc/mysql/my.cnf
/etc/init.d/mysql stop
/etc/init.d/mysql start