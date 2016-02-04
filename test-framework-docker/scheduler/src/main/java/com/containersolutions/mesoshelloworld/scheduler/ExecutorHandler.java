package com.containersolutions.mesoshelloworld.scheduler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handler which serves executor Jar.
 */
public class ExecutorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        InputStream executorJarResource = ExecutorHandler.class.getClassLoader().getResourceAsStream("mesos-hello-world-executor.jar");
        byte[] executorJar = IOUtils.toByteArray(executorJarResource);
        httpExchange.sendResponseHeaders(200, executorJar.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(executorJar);
        os.close();
    }
}
