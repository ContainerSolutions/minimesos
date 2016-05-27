package com.containersol.minimesos.api;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import static spark.Spark.exception;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Start the REST API
 */
public class Main {

    private ClusterRepository repository;
    private final MesosClusterContainersFactory factory;

    public Main() {
        repository = new ClusterRepository();
        factory = new MesosClusterContainersFactory();
    }

    public static void main(String[] args) throws UnknownHostException {
        Main main = new Main();
        main.run();
    }

    private void run() throws UnknownHostException {
        port(8080);

        post("/start", "text/plain", (request, response) -> {
            ClusterConfig clusterConfig = new ConfigParser().parse(request.body());
            MesosCluster mesosCluster = factory.createMesosCluster(clusterConfig);
            mesosCluster.start();
            return new ClusterStartedResponse(mesosCluster.getClusterId());
        }, JsonUtils.json());

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
        });

        System.out.println("minimesos is running on http://" + Inet4Address.getLocalHost().getHostAddress());
    }
}
