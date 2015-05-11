 #
 # Licensed to the Camel Labs under one or more
 # contributor license agreements.  See the NOTICE file distributed with
 # this work for additional information regarding copyright ownership.
 # The licenses this file to You under the Apache License, Version 2.0
 # (the "License"); you may not use this file except in compliance with
 # the License.  You may obtain a copy of the License at
 #
 #      http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.

 #!/usr/bin/env bash

sudo wget https://raw.githubusercontent.com/camel-labs/camel-labs/master/iot/initd/rasbian/camel-iot-gateway.sh -O /etc/init.d/camel-iot-gateway
sudo chmod +x /etc/init.d/camel-iot-gateway
sudo update-rc.d /etc/init.d/camel-iot-gateway defaults

sudo /etc/init.d/camel-iot-gateway start