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
package io.rhiot.gateway.test;

import io.rhiot.gateway.Gateway;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;

import static io.rhiot.utils.Properties.restoreSystemProperties;

public abstract class GatewayTest extends Assert {

    protected static Gateway gateway;

    @Before
    public void before() {
        doBefore();
        if(gateway == null) {
            gateway = new Gateway().start();
        }
    }

    protected void doBefore() {
    }

    @AfterClass
    public static void after() {
        try {
            gateway.stop();
        } finally {
            gateway = null;
            restoreSystemProperties();
        }
    }

}