/**
 * Licensed to the Camel Labs under one or more
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
package com.github.camellabs.iot.cloudlet.device

import com.github.camellabs.iot.cloudlet.device.leshan.MongoDbClientRegistry
import com.mongodb.Mongo
import io.vertx.core.Future
import io.vertx.lang.groovy.GroovyVerticle
import org.eclipse.leshan.core.node.LwM2mResource
import org.eclipse.leshan.core.request.ReadRequest
import org.eclipse.leshan.core.response.LwM2mResponse
import org.eclipse.leshan.core.response.ValueResponse
import org.eclipse.leshan.server.californium.LeshanServerBuilder

import static com.github.camellabs.iot.cloudlet.device.vertx.Vertxes.wrapIntoJsonResponse
import static org.eclipse.leshan.ResponseCode.CONTENT

class LeshanServerVeritcle extends GroovyVerticle {

    final def mongo = new Mongo()

    final def leshanServer = new LeshanServerBuilder().setClientRegistry(new MongoDbClientRegistry(mongo)).build()

    @Override
    void start(Future<Void> startFuture) throws Exception {
        vertx.runOnContext {
            leshanServer.start()

            vertx.eventBus().localConsumer('listClients') { msg ->
                wrapIntoJsonResponse(msg, 'clients', leshanServer.clientRegistry.allClients())
            }

            vertx.eventBus().localConsumer('deleteClients') { msg ->
                leshanServer.clientRegistry.allClients().each {
                        client -> leshanServer.clientRegistry.deregisterClient(client.registrationId) }
                wrapIntoJsonResponse(msg, 'Status', 'Success')
            }

            vertx.eventBus().localConsumer('getClient') { msg ->
                wrapIntoJsonResponse(msg, 'client', leshanServer.clientRegistry.get(msg.body().toString()))
            }

            vertx.eventBus().localConsumer('client.manufacturer') { msg ->
                def clientId = msg.body().toString()
                def client = leshanServer.clientRegistry.get(clientId)
                if(client == null) {
                    msg.fail(0, "No client with ID ${clientId}.")
                } else {
                    wrapIntoJsonResponse(msg, 'manufacturer', stringResponse(leshanServer.send(client, new ReadRequest('/3/0/0'))))
                }
            }

            startFuture.complete()
        }
    }

    // Helpers

    private String stringResponse(LwM2mResponse response) {
        if(response.code != CONTENT || !(response instanceof ValueResponse)) {
            return null
        }
        def content = response.asType(ValueResponse.class).content
        if(!(content instanceof LwM2mResource)) {
            return null
        }
        content.asType(LwM2mResource).value.value
    }

}
