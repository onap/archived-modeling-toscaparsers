#!/usr/bin/env bash

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

# Client constraint file contains this client version pin that is in conflict
# with installing the client from source. We should remove the version pin in
# the constraints file before applying it for from-source installation.

CONSTRAINTS_FILE="$1"
shift 1

set -e

# NOTE(tonyb): Place this in the tox enviroment's log dir so it will get
# published to logs.openstack.org for easy debugging.
localfile="$VIRTUAL_ENV/log/upper-constraints.txt"

if [[ "$CONSTRAINTS_FILE" != http* ]]; then
    CONSTRAINTS_FILE="file://$CONSTRAINTS_FILE"
fi
# NOTE(tonyb): need to add curl to bindep.txt if the project supports bindep
curl "$CONSTRAINTS_FILE" --insecure --progress-bar --output "$localfile"

pip install -c"$localfile" openstack-requirements

# This is the main purpose of the script: Allow local installation of
# the current repo. It is listed in constraints file and thus any
# install will be constrained and we need to unconstrain it.
edit-constraints "$localfile" -- "$CLIENT_NAME"

pip install -c"$localfile" -U "$@"
exit $?
