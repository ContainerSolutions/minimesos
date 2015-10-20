package com.containersol.minimesos.marathon;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Client to talk to Marathon's API
 */
public class MarathonClient {

    private static Logger LOGGER = Logger.getLogger(MarathonClient.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Usage MarathonClient killall <ip> | deploy <ip> <marathon.json>");
        } else {
            switch(args[0]) {
                case "killall":
                    try {
                        String marathonIp = getMarathonIp(args[1]);
                        killAllApps(marathonIp);
                    } catch (UnknownHostException e) {
                        LOGGER.error("Not an ip address! Usage: MarathonClient killall <ip>");
                        System.exit(1);
                    }
                    break;
                case "deploy":
                    if (args.length != 3) {
                        LOGGER.error("Usage: MarathonClient deploy <ip> <marathon.json>");
                    }
                    try {
                        String marathonIp = getMarathonIp(args[1]);
                        File marathonFile = new File(args[2]);
                        if (!marathonFile.exists()) {
                            System.out.println("No such file " + args[2] + ". Usage: MarathonClient deploy <ip> <marathon.json>");
                        }
                        MarathonClient.deployFramework(marathonIp, args[2]);
                    } catch (UnknownHostException e) {
                        System.out.println("Not an ip address! Usage: MarathonClient killall <ip>");
                        System.exit(1);
                    }
                    break;
            }
        }
    }

    private static String getMarathonIp(String arg) throws UnknownHostException {
        return InetAddress.getByName(arg).getHostAddress();
    }

    /**
     * Kill all apps that are currently running.
     */
    public static void killAllApps(String marathonIp) {
        String marathonEndpoint = "http://" + marathonIp + ":8080";
        JSONObject appsResponse;
        try {
            appsResponse = Unirest.get(marathonEndpoint + "/v2/apps").header("accept", "application/json").asJson().getBody().getObject();
            if (appsResponse.length() == 0) {
                return;
            }
        } catch (UnirestException e) {
            LOGGER.error("Could not retrieve apps from Marathon at " + marathonEndpoint);
            return;
        }

        JSONArray apps = appsResponse.getJSONArray("apps");
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.getJSONObject(i);
            String appId = app.getString("id");
            try {
                Unirest.delete(marathonEndpoint + "/v2/apps" + appId).asJson();
            } catch (UnirestException e) {
                LOGGER.error("Could not delete app " + appId + " at " + marathonEndpoint);
            }
        }
    }

    public static void deployFramework(String marathonIp, String marathonFile) {
        String marathonEndpoint = "http://" + marathonIp + ":8080";
        JSONObject deployResponse;
        try (FileInputStream fis = new FileInputStream(marathonFile)) {
            deployResponse = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(IOUtils.toByteArray(fis)).asJson().getBody().getObject();
            LOGGER.info(deployResponse);
        } catch (UnirestException | IOException e) {
            LOGGER.error("Could not deploy framework on Marathon at " + marathonEndpoint);
            System.exit(1);
        }
    }
}
