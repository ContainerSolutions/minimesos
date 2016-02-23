package com.containersol.minimesos.restapi;

import org.apache.log4j.Logger;
import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.port;

/**
 * Starts the minimesos REST API
 */
public class RestApi {

    public static final Logger LOGGER = Logger.getLogger(RestApi.class);

    public static void main(String[] args) {
        RestApi restApi = new RestApi(9000);
    }

    public RestApi(int port) {
        port(port);

        LOGGER.info("Starting minimesos REST API on port " + port);

        get("/install", (req, res) -> "This is the 'install' endpoint");
        get("/state", (req, res) -> "This is the 'state' endpoint");
        get("/info", (req, res) -> "This is the 'info' endpoint");
        get("/help", (req, res) -> "This is the 'help' endpoint");
    }

    private void stop() {
        Spark.stop();
    }

}
