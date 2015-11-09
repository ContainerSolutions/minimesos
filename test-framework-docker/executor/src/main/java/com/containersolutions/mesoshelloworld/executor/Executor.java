package com.containersolutions.mesoshelloworld.executor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Adapted from https://github.com/apache/mesos/blob/0.22.1/src/examples/java/TestExecutor.java
 */
public class Executor implements org.apache.mesos.Executor {
    private Thread thread;

    public static void main(String[] args) throws Exception {
        MesosExecutorDriver driver = new MesosExecutorDriver(new Executor());
        System.exit(driver.run() == Status.DRIVER_STOPPED ? 0 : 1);
    }

    @Override
    public void registered(ExecutorDriver driver,
                           ExecutorInfo executorInfo,
                           FrameworkInfo frameworkInfo,
                           SlaveInfo slaveInfo) {
        System.out.println("Registered executor on " + slaveInfo.getHostname());
    }

    @Override
    public void reregistered(ExecutorDriver driver, SlaveInfo executorInfo) {
    }

    @Override
    public void disconnected(ExecutorDriver driver) {
    }

    @Override
    public void launchTask(final ExecutorDriver driver, final TaskInfo task) {
        thread = new Thread() {
            public void run() {
                try {
                    Integer port = task.getDiscovery().getPorts().getPorts(0).getNumber();
                    System.out.println("Starting webserver on port " + port);
                    startWebServer(port);

                    TaskStatus status = TaskStatus.newBuilder()
                            .setTaskId(task.getTaskId())
                            .setState(TaskState.TASK_RUNNING).build();

                    driver.sendStatusUpdate(status);

                    System.out.println("Running task " + task.getTaskId().getValue());
                } catch (Exception e) {
                    System.out.println("Unable to start webserver:" + e);

                    TaskStatus status = TaskStatus.newBuilder()
                            .setTaskId(task.getTaskId())
                            .setState(TaskState.TASK_FINISHED).build();

                    driver.sendStatusUpdate(status);
                }
            }
        };
        thread.start();
    }

    private void startWebServer(Integer port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    @Override
    public void killTask(ExecutorDriver driver, TaskID taskId) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Override
    public void frameworkMessage(ExecutorDriver driver, byte[] data) {
    }

    @Override
    public void shutdown(ExecutorDriver driver) {
    }

    @Override
    public void error(ExecutorDriver driver, String message) {
    }

    private class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Hello world";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
