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
package com.github.camellabs.iot.cloudlet.geofencing.service;

import com.github.camellabs.iot.cloudlet.document.driver.spi.DocumentDriver;
import com.github.camellabs.iot.cloudlet.document.driver.spi.SaveOperation;
import com.github.camellabs.iot.cloudlet.geofencing.GeofencingCloudlet;
import com.github.camellabs.iot.cloudlet.geofencing.domain.GpsCoordinates;
import com.github.camellabs.iot.cloudlet.geofencing.domain.Route;
import com.github.camellabs.iot.cloudlet.geofencing.googlemaps.StaticMaps;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.LatLng;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.ONE_MINUTE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.util.Collections.singletonList;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {GeofencingCloudlet.class, DefaultRouteServiceTest.class})
@IntegrationTest({"camel.labs.iot.cloudlet.document.driver.mongodb.embedded=true"})
public class DefaultRouteServiceTest extends Assert {

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    RouteService routeService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    DocumentDriver documentDriver;

    static int restPort = findAvailableTcpPort();

    String restApi = "http://localhost:" + restPort + "/api/geofencing/";

    String client = "client";

    GpsCoordinates point1 = new GpsCoordinates(null, client, "clientId", new Date(), TEN, TEN);
    GpsCoordinates point2 = new GpsCoordinates(null, client, "clientId", new Date(), TEN.add(ONE), TEN.add(ONE));
    GpsCoordinates point3 = new GpsCoordinates(null, client, "clientId", new DateTime(point2.getTimestamp()).plusMinutes(6).toDate(), TEN.add(ONE), TEN.add(ONE));

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("server.port", findAvailableTcpPort() + "");
        System.setProperty("camel.labs.iot.cloudlet.rest.port", restPort + "");

        int mongodbPort = findAvailableTcpPort();
        System.setProperty("camel.labs.iot.cloudlet.document.driver.mongodb.embedded.port", mongodbPort + "");
        System.setProperty("camel.labs.iot.cloudlet.document.driver.mongodb.springbootconfig", TRUE.toString());
        System.setProperty("spring.data.mongodb.port", mongodbPort + "");
    }


    @Before
    public void before() {
        mongoTemplate.getDb().dropDatabase();
    }

    // Tests

    @Test
    public void shouldReturnNoClientsForEmptyDatabase() {
        assertEquals(0, routeService.clients().size());
    }

    @Test
    public void shouldReturnClients() throws URISyntaxException {
        // Given
        documentDriver.save(new SaveOperation(point1));
        URI clientsRequestUri = new URI(restApi + "routes/clients");

        // When
        @SuppressWarnings("unchecked")
        Map<String, List<String>> clients = restTemplate.getForObject(clientsRequestUri, Map.class);

        // Then
        assertEquals(singletonList(client), clients.get("clients"));
    }

    @Test
    public void shouldReturnRoutes() throws URISyntaxException {
        // Given
        documentDriver.save(new SaveOperation(point1));
        routeService.analyzeRoutes(client);
        URI clientsRequestUri = new URI(restApi + "routes/routes/" + client);

        // When
        @SuppressWarnings("unchecked")
        Map<String, List<String>> routes = restTemplate.getForObject(clientsRequestUri, Map.class);

        // Then
        assertEquals(1, routes.get("routes").size());
    }

    @Test
    public void shouldNotFailForEmptyDatabase() {
        assertEquals(0, routeService.analyzeRoutes(client));
        assertEquals(0, routeService.routes(client).size());
    }

    @Test
    public void shouldIdempotentlyAssignSinglePointToRoute() {
        documentDriver.save(new SaveOperation(point1));
        assertEquals(1, routeService.analyzeRoutes(client));
        assertEquals(0, routeService.analyzeRoutes(client));
        assertEquals(1, routeService.routes(client).size());
    }

    @Test
    public void shouldIdempotentlyAssignTwoPointsToRoute() {
        documentDriver.save(new SaveOperation(point1));
        assertEquals(1, routeService.analyzeRoutes(client));
        assertEquals(0, routeService.analyzeRoutes(client));
        assertEquals(1, routeService.routes(client).size());

        documentDriver.save(new SaveOperation(point2));
        assertEquals(1, routeService.analyzeRoutes(client));
        assertEquals(0, routeService.analyzeRoutes(client));
        assertEquals(1, routeService.routes(client).size());
    }

    @Test
    public void shouldDetectNextRoute() {
        documentDriver.save(new SaveOperation(point1));
        documentDriver.save(new SaveOperation(point2));
        documentDriver.save(new SaveOperation(point3));

        assertEquals(3, routeService.analyzeRoutes(client));

        assertEquals(2, routeService.routes(client).size());
    }

    @Test
    public void shouldTriggerRouteAnalysisTimer() {
        // Given
        documentDriver.save(new SaveOperation(point1));

        // When
        await().atMost(ONE_MINUTE).until(() -> !routeService.routes(client).isEmpty());

        // Then
        assertEquals(1, routeService.routes(client).size());
    }

    @Test
    public void shouldDrawRouteOnMap() throws URISyntaxException, MalformedURLException {
        // Given
        documentDriver.save(new SaveOperation(point1));
        documentDriver.save(new SaveOperation(point2));
        routeService.analyzeRoutes(client);
        URI clientsRequestUri = new URI(restApi + "routes/routes/" + client);
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String,String>>> routes = restTemplate.getForObject(clientsRequestUri, Map.class);
        URI routeUrlRequest = new URI(restApi + "routes/routeUrl/" + routes.get("routes").get(0).get("id"));

        // When
        String routeUrl = restTemplate.getForObject(routeUrlRequest, Map.class).get("routeUrl").toString();

        // Then
        String encodedCoordinates = StaticMaps.extractEncodedPath(new URL(routeUrl));
        List <LatLng> coordinates = PolylineEncoding.decode(encodedCoordinates);
        assertEquals(coordinates.get(0).lat, point1.getLatitude().doubleValue(), 0.1);
        assertEquals(coordinates.get(0).lng, point1.getLongitude().doubleValue(), 0.1);
        assertEquals(coordinates.get(1).lat, point2.getLatitude().doubleValue(), 0.1);
        assertEquals(coordinates.get(1).lng, point2.getLongitude().doubleValue(), 0.1);

    }

}