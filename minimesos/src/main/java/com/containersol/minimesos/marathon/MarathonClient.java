package com.containersol.minimesos.marathon;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Client to talk to Marathon's API
 */
public class MarathonClient {

    private static Logger LOGGER = Logger.getLogger(MarathonClient.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.printf("Usage MarathonClient <ip>");
        } else {
            try {
                String marathonIp = InetAddress.getByName(args[0]).getHostAddress();
                killAllApps(marathonIp);
            } catch (UnknownHostException e) {
                System.out.println("Not an ip address!");
                System.out.println("Usage MarathonClient <ip>");
            }
        }
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

}
