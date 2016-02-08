/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejbinwar.test;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.quickstarts.ejbinwar.controller.Greeter;
import org.jboss.as.quickstarts.ejbinwar.ejb.GreeterEJB;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A simple test case using Arquillian to test a CDI bean.
 *
 * @author david@davidsalter.co.uk
 */
@RunWith(Arquillian.class)
public class GreeterTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(Greeter.class, GreeterEJB.class)
                .addPackages(true, "org.apache.http")
            .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
    }

    @Inject
    Greeter greeter;

    @Test
    public void testGetMessage() throws Exception {
        String name = "World!";
        greeter.setName(name);

        assertEquals("Hello " + name, greeter.getMessage());
    }

    @Test
    //@RunAsClient
    public void testPing() throws Exception {
        System.out.println(url);
        final HttpClient client = HttpClientBuilder.create()
                .build();
        final String target = url.toExternalForm() + "index.jsf";
        System.out.println(target);
        final HttpResponse response = client.execute(new HttpGet(url.toURI()));

        System.out.println("Response Code : "
                + response.getStatusLine().getStatusCode());

        /*BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }*/
    }
}
