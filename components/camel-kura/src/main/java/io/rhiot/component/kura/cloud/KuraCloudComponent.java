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

import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.kura.cloud.CloudService;

public class KuraCloudComponent extends UriEndpointComponent {

    public KuraCloudComponent() {
        super(KuraCloudEndpoint.class);
    }

    public KuraCloudComponent(CamelContext context, Class<? extends Endpoint> endpointClass) {
        super(context, endpointClass);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remain, Map<String, Object> parameters) throws Exception {

        Set<CloudService> cloudServiceSet = this.getCamelContext().getRegistry().findByType(CloudService.class);
        CloudService cloudService = null;

        if (cloudServiceSet.size() == 1) {
            for (CloudService cloudServiceIt : cloudServiceSet) {
                cloudService = cloudServiceIt;
            }
        } else {
            throw new IllegalArgumentException("");
        }

        KuraCloudEndpoint kuraCloudEndpoint = new KuraCloudEndpoint(uri, this, cloudService);

        String[] res = remain.split("/");
        if (res.length != 2) {
            throw new IllegalArgumentException("");
        }
        parameters.put(KuraCloudConstants.APPLICATION_ID, res[0]);
        parameters.put(KuraCloudConstants.TOPIC, res[1]);

        setProperties(kuraCloudEndpoint, parameters);

        return kuraCloudEndpoint;
    }

}
