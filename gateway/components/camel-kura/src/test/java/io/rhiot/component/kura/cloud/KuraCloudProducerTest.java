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

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Before;
import org.junit.Test;

import com.google.common.truth.Truth;

public class KuraCloudProducerTest extends CamelTestSupport {

    CloudService cloudService = mock(CloudService.class);

    CloudClient cloudClient = mock(CloudClient.class);

    @Before
    public void before() throws KuraException {
        given(cloudService.newCloudClient(anyString())).willReturn(cloudClient);
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();
        registry.bind("cloudService", cloudService);
        return registry;
    }

    @Test
    public void shouldSendKuraPayloadToTopic() throws KuraException {
        // Given
        KuraPayload kuraPayload = new KuraPayload();

        // When
        template.sendBody("kura-cloud:app/topic", kuraPayload);

        // Then
        verify(cloudClient).publish(eq("topic"), eq(kuraPayload), anyInt(), anyBoolean(), anyInt());
    }

    @Test
    public void shouldReuseCacheClient() throws Exception {
        CloudClient firstCloudClient = context.getEndpoint("kura-cloud:app/topic1", KuraCloudEndpoint.class)
                .createProducer().cloudClient;
        CloudClient secondCloudClient = context.getEndpoint("kura-cloud:app/topic2", KuraCloudEndpoint.class)
                .createProducer().cloudClient;
        Truth.assertThat(firstCloudClient).isSameAs(secondCloudClient);
    }
}