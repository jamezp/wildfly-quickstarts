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
package org.jboss.as.quickstarts.helloworld;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * A simple servlet taking advantage of features added in 3.0.
 * </p>
 *
 * <p>
 * The servlet is registered and mapped to /HelloServlet using the {@linkplain WebServlet
 *
 * @author Pete Muir
 * @HttpServlet}. The {@link HelloService} is injected by CDI.
 * </p>
 */
@SuppressWarnings("serial")
@WebServlet("/HelloWorld")
public class HelloWorldServlet extends HttpServlet {

    static String PAGE_HEADER = "<html><head><title>helloworld</title></head><body>";

    static String PAGE_FOOTER = "</body></html>";

    @Inject
    HelloService helloService;

    @Resource
    private ManagedExecutorService managedExecutorService;

    @Resource
    private ManagedScheduledExecutorService managedScheduledExecutorService;

    private final Queue<Future<?>> scheduledTasks = new LinkedBlockingDeque<>();

    @Override
    public void destroy() {
        Future<?> future;
        while ((future = scheduledTasks.poll()) != null) {
            future.cancel(true);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        final PrintWriter writer = resp.getWriter();
        final HelloService helloService = this.helloService;
        try {
            managedExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    writer.println(PAGE_HEADER);
                    writer.println("<h1>" + helloService.createHelloMessage("Browser") + "</h1>");
                    writer.println(PAGE_FOOTER);
                    writer.close();
                }
            }).get();
        } catch (Throwable e) {
            throw new ServletException(e);
        }
        for (int i = 1; i < 6; i++) {
            final int j = i;
            managedScheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println(helloService.createHelloMessage("Console") + " " + j);
                }
            }, 1, TimeUnit.SECONDS);
        }

        scheduledTasks.add(managedScheduledExecutorService.scheduleAtFixedRate(() -> System.out.printf("Ran at %s%n", ZonedDateTime.now()),
                1, 3, TimeUnit.SECONDS));

    }
}
