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
package org.apache.camel.component.spark;

import org.apache.camel.component.spark.annotations.RddCallback;
import org.apache.spark.api.java.AbstractJavaRDDLike;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.camel.util.ObjectHelper.findMethodsWithAnnotation;

public class AnnotatedRddCallback {

    public static org.apache.camel.component.spark.RddCallback annotatedRddCallback(Object callback) {
        List<Method> rddCallbacks = findMethodsWithAnnotation(callback.getClass(), RddCallback.class);
        if(rddCallbacks.size() > 0 ) {
            return new org.apache.camel.component.spark.RddCallback() {
                @Override
                public Object onRdd(AbstractJavaRDDLike rdd, Object... payloads) {
                    try {
                        List<Object> arguments = new ArrayList<>(payloads.length + 1);
                        arguments.add(rdd);
                        arguments.addAll(asList(payloads));
                        if(arguments.get(1) == null) {
                            arguments.remove(1);
                        }
                        return rddCallbacks.get(0).invoke(callback, arguments.toArray(new Object[0]));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        throw new UnsupportedOperationException("Can't find methods annotated with @Rdd.");
    }

}
