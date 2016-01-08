/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhiot.cloudplatform.adapter.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import io.rhiot.datastream.engine.ServiceBinding;
import io.rhiot.datastream.engine.test.DataStreamTest;
import io.vertx.core.json.Json;
import org.junit.Test;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.util.Map;


public class RestProtocolAdapterTest extends DataStreamTest {

    RestTemplate rest = new RestTemplate();

    @Test
    public void shouldInvokeGetOperation() throws IOException {
        Map response = Json.mapper.readValue(new URL("http://localhost:8080/test/count/1"), Map.class);
        Truth.assertThat(response.get("payload")).isEqualTo(1);
    }

    @Test
    public void shouldInvokePostOperation() {
        byte[] request = payloadEncoding.encode(ImmutableMap.of("foo", "bar"));
        Object payload = rest.postForObject("http://localhost:8080/test/sizeOf", request, Map.class).get("payload");
        Truth.assertThat(payload).isEqualTo(1);
    }

    // Beans fixtures

    public static interface TestService {

        int count(int number);

        int sizeOf(Map map);

    }

    @Component("test")
    public static class TestInterfaceImpl implements TestService {

        @Override
        public int count(int number) {
            return number;
        }

        @Override
        public int sizeOf(Map map) {
            return map.size();
        }

    }

    @Component
    public static class TestInterfaceServiceBinding extends ServiceBinding {

        TestInterfaceServiceBinding() {
            super("test");
        }

    }

}