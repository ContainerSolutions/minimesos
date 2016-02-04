package com.containersolutions.mesoshelloworld.scheduler;

import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpServer;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;

import java.net.InetSocketAddress;

/**
 */
public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/exectutor", new ExecutorHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        Configuration configuration = new Configuration(args);

        Protos.FrameworkInfo.Builder frameworkBuilder = Protos.FrameworkInfo.newBuilder()
                .setUser("") // Have Mesos fill in the current user.
                .setName("Hello world example")
                .setCheckpoint(true);

        String principal = configuration.getFrameworkPrincipal();
        if (principal != null) {
            frameworkBuilder.setPrincipal(principal);
        }

        org.apache.mesos.Scheduler scheduler = new Scheduler(configuration);

        Protos.FrameworkInfo frameworkInfo = frameworkBuilder.build();
        String mesosMaster = configuration.getMesosMaster();

        MesosSchedulerDriver driver =
                principal != null
              ? new MesosSchedulerDriver(
                        scheduler,
                        frameworkInfo,
                        mesosMaster,
                        Protos.Credential.newBuilder()
                                .setPrincipal( principal )
                                .setSecret(ByteString.copyFromUtf8(configuration.getFrameworkSecret()))
                                .build()
                )
              : new MesosSchedulerDriver(
                    scheduler,
                    frameworkInfo,
                    mesosMaster
                );

        // Ensure that the driver process terminates.
        driver.stop();

        if (driver.run() != Protos.Status.DRIVER_STOPPED) {
            throw new RuntimeException("Mesos Scheduler Driver is not stopped");
        }

    }
}
