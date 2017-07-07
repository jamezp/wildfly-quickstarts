/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.quickstarts.helloworld;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/log-rotate")
public class LoggingRotateServlet extends HttpServlet {
    private static final String LOGGER_NAME = LoggingRotateServlet.class.getPackage().getName();

    private final ModelNode handlerAddress = Operations.createAddress("subsystem", "logging", "periodic-rotating-file-handler", "test");
    private final ModelNode loggerAddress = Operations.createAddress("subsystem", "logging", "logger", LOGGER_NAME);
    private final Path logDir = Paths.get(System.getProperty("jboss.server.log.dir"));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        // Get the configuration parameters
        final String host = getOrDefault(req.getParameter("host"), "127.0.0.1");
        final int port = getOrDefault(req.getParameter("port"), 9990);
        final String suffix = getOrDefault(req.getParameter("suffix"), ".yyyy-MM");
        final String formatterName = getOrDefault(req.getParameter("formatterName"), "PATTERN");
        final int iterations = getOrDefault(req.getParameter("iter"), 20);
        final int months = getOrDefault(req.getParameter("months"), 3);

        // Configure the handler
        configureLogHandler(host, port, suffix, formatterName);

        // Write logs incrementing the the months to test rotation
        final Logger logger = Logger.getLogger(LOGGER_NAME);
        for (int month = 0; month < months; month++) {
            for (int i = 0; i < iterations; i++) {
                final LogRecord logRecord = new LogRecord(Level.INFO, "This is test message " + i);
                logRecord.setLoggerName(LOGGER_NAME);
                logRecord.setMillis(ZonedDateTime.now().plusMonths(month).toInstant().toEpochMilli());
                logger.log(logRecord);
            }
        }

        try (JsonGenerator generator = Json.createGenerator(resp.getWriter())) {
            generator.writeStartObject();
            for (Path file : getFiles()) {
                generator.writeStartObject(file.getFileName().toString());
                final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                generator.writeStartArray("lines");
                lines.forEach(generator::write);
                generator.writeEnd(); // end array
                generator.writeEnd(); // end file name object
            }
            generator.writeEnd(); // end main
        }

        // Remove handler configuration
        removeLogHandler(host, port);
    }

    private void configureLogHandler(final String host, final int port, final String suffix, final String formatterName) throws IOException {
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

        // Add the handler
        final ModelNode addHandler = Operations.createAddOperation(handlerAddress);
        addHandler.get("autoflush").set(true);
        addHandler.get("append").set(true);
        addHandler.get("named-formatter").set(formatterName);
        addHandler.get("suffix").set(suffix);
        final ModelNode file = new ModelNode().setEmptyObject();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set("test.log");
        addHandler.get("file").set(file);
        builder.addStep(addHandler);

        // Add the logger
        final ModelNode addLogger = Operations.createAddOperation(loggerAddress);
        addLogger.get("handlers").set(new ModelNode().setEmptyList().add("test"));
        addLogger.get("use-parent-handlers").set(false);
        builder.addStep(addLogger);

        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            final ModelNode result = client.execute(builder.build());
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException(Operations.getFailureDescription(result).asString());
            }
        }
    }

    private void removeLogHandler(final String host, final int port) throws IOException {
        final CompositeOperationBuilder builder = CompositeOperationBuilder.create();
        builder.addStep(Operations.createRemoveOperation(loggerAddress));
        builder.addStep(Operations.createRemoveOperation(handlerAddress));
        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            final ModelNode result = client.execute(builder.build());
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new RuntimeException(Operations.getFailureDescription(result).asString());
            }
        }
        // Remove all the log files
        for (Path path : getFiles()) {
            Files.deleteIfExists(path);
        }
    }

    private Iterable<Path> getFiles() throws IOException {
        final List<Path> result = new ArrayList<>();
        Files.walkFileTree(logDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().startsWith("test.log")) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(result);
        return result;
    }

    private static int getOrDefault(final String value, final int dft) {
        return value == null ? dft : Integer.parseInt(value);
    }

    private static String getOrDefault(final String value, final String dft) {
        return value == null ? dft : value;
    }
}
