/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.quickstarts.microprofile.reactive.messaging.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.quickstarts.microprofile.reactive.messaging.UserMessagingBean;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({/*RunKafkaSetupTask.class, */EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingKafkaIT {

    @ArquillianResource
    URL url;

    private final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    private static final long TIMEOUT = 30000;

    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-tx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(UserMessagingBean.class.getPackage());

        return webArchive;
    }

    @Test
    public void test() throws Throwable {
        HttpGet httpGet = new HttpGet(url.toExternalForm());
        long end = System.currentTimeMillis() + TIMEOUT;
        boolean done = false;
        while (!done && System.currentTimeMillis() < end) {
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                done = checkResponse(httpResponse, System.currentTimeMillis() > end);
                Thread.sleep(1000);
            }
        }
    }

    @Test
    public void testUserApi() throws Throwable {
        String userUrl = url.toExternalForm() + "user";
        ReadAsynchronousTask taskA = new ReadAsynchronousTask(httpClient, userUrl);
        executorService.submit(taskA);
//        ReadAsynchronousTask taskB = new ReadAsynchronousTask(httpClient, userUrl);
//        executorService.submit(taskB);

        taskA.latch.await();
//        taskB.latch.await();

        post(userUrl + "/one");
        post(userUrl + "/two");
        post(userUrl + "/three");

        long end = System.currentTimeMillis() + TIMEOUT;
        while (System.currentTimeMillis() < end) {
            Thread.sleep(200);
            if (taskA.lines.size() == 3) {
                break;
            }
        }

        checkAsynchTask(taskA, "one", "two", "three");
//        checkAsynchTask(taskB, "one", "two", "three");
    }

    private void checkAsynchTask(ReadAsynchronousTask task, String...values) {
        Assert.assertEquals(3, task.lines.size());
        for (int i = 0; i < values.length; i++) {
            Assert.assertTrue("Line " + i + ": " + task.lines.get(i), task.lines.get(i).contains(values[i]));
        }
    }

    private void post(String url) throws Exception {
        HttpPost post = new HttpPost(url);
        try (CloseableHttpResponse httpResponse = httpClient.execute(post)) {
            String sc = String.valueOf(httpResponse.getStatusLine().getStatusCode());
            Assert.assertTrue(sc, sc.startsWith("2"));
        }
    }

    private boolean checkResponse(CloseableHttpResponse response, boolean fail) throws Throwable {
        String s;
        List<String> lines = new ArrayList<>();
        try {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                String line = reader.readLine();
                while (line != null) {
                    lines.add(line);
                    line = reader.readLine();
                }
            }
            Assert.assertTrue("Expected >= 3 lines in:\n" + lines, lines.size() >= 3);
        } catch (Throwable throwable) {
            if (fail) {
                throw throwable;
            }
            return false;
        }

        Assert.assertNotEquals("Expected to find 'Hello' on line 0 of:\n" + lines, -1, lines.get(0).indexOf("Hello"));
        Assert.assertNotEquals("Expected to find 'Kafka' on line 1 of:\n" + lines, -1, lines.get(1).indexOf("Kafka"));
        for (int i = 2; i < lines.size(); i++) {
            Assert.assertNotEquals(
                    "Expected to find 'Hello' or 'Kafka' on line " + i +
                            " of:\n" + lines, -2, lines.get(i).indexOf("Hello") + lines.get(i).indexOf("Kafka"));
        }
        return true;
    }

    private static class ReadAsynchronousTask implements Runnable {
        private CloseableHttpClient httpClient;
        private final String url;
        private final CountDownLatch latch = new CountDownLatch(1);
        private List<String> lines = Collections.synchronizedList(new ArrayList<>());

        public ReadAsynchronousTask(CloseableHttpClient httpClient, String url) {
            this.httpClient = httpClient;
            this.url = url;
        }

        @Override
        public void run() {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()))) {
                    latch.countDown();

                    String line = reader.readLine();
                    while (line != null) {
                        Thread.sleep(100);
                        if (line.trim().length() > 0) {
                            lines.add(line);
                        }
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (InterruptedException e) {
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}
