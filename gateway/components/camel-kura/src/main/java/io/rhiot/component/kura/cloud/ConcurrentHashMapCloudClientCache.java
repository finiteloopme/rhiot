/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhiot.component.kura.cloud;

import org.eclipse.kura.cloud.CloudClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapCloudClientCache implements CloudClientCache {

    private final Map<String, CloudClient> cacheMap = new ConcurrentHashMap<>();

    @Override
    public void put(String appId, CloudClient cloudClient) {
        cacheMap.put(appId, cloudClient);
    }

    @Override
    public CloudClient get(String appId) {
        return cacheMap.get(appId);
    }

}