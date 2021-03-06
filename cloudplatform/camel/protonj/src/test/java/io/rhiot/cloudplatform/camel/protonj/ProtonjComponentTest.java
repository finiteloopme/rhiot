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
package io.rhiot.cloudplatform.camel.protonj;

import com.google.common.truth.Truth;
import org.apache.activemq.broker.BrokerService;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.apache.camel.component.amqp.AMQPComponent.amqp10Component;
import static org.apache.camel.test.AvailablePortFinder.getNextAvailable;

public class ProtonjComponentTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:foo")
    MockEndpoint mockEndpoint;

    @EndpointInject(uri = "mock:mytopic")
    MockEndpoint mytopicMockEndpoint;

    int peerConsumerPort = getNextAvailable();

    @BeforeClass
    public static void beforeClass() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.addConnector("amqp://0.0.0.0:9999");
        broker.start();
    }

    String message = randomUUID().toString();

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        context.addComponent("amqp", amqp10Component("amqp://guest:guest@localhost:9999"));
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("protonj:amqp://~0.0.0.0:" + peerConsumerPort).to("mock:foo");

                from("amqp:topic:mytopic").to("mock:mytopic");
            }
        };
    }

    // Peer2peer tests

    @Test
    public void shouldReceivePeer2PeerMessage() throws InterruptedException {
        mockEndpoint.expectedBodiesReceived(message);
        template.sendBody("protonj:amqp://0.0.0.0:" + peerConsumerPort, message);
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldReceivePeer2PeerMessages() throws InterruptedException {
        mockEndpoint.expectedBodiesReceived(message, message);
        template.sendBody("protonj:amqp://0.0.0.0:" + peerConsumerPort, message);
        template.sendBody("protonj:amqp://0.0.0.0:" + peerConsumerPort, message);
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldReceivePeer2PeerMapMessage() throws InterruptedException {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put(message, message);
        mockEndpoint.expectedBodiesReceived(payload);
        template.sendBody("protonj:amqp://0.0.0.0:" + peerConsumerPort, payload);
        mockEndpoint.assertIsSatisfied();
    }

    // Peer2broker tests

    @Test
    public void shouldSendMessageToBrokerQueue() throws InterruptedException {
        template.sendBody("protonj:localhost:9999/foo", "foo");
        String receivedMessage = consumer.receiveBody("amqp:foo", String.class);
        Truth.assertThat(receivedMessage).isEqualTo("foo");
    }

    @Test
    public void shouldSendMessageToBrokerTopic() throws InterruptedException {
        mytopicMockEndpoint.expectedBodiesReceived("foo");
        template.sendBody("protonj:amqp://localhost:9999/topic://mytopic", "foo");
        mytopicMockEndpoint.assertIsSatisfied();
    }

}
