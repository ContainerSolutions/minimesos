package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MinimesosException;
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

    public static void deployFramework(String marathonIp, File marathonFile) {
        String marathonEndpoint = "http://" + marathonIp + ":" + Marathon.MARATHON_PORT;
        JSONObject deployResponse;
        try (FileInputStream fis = new FileInputStream(marathonFile)) {
            deployResponse = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(IOUtils.toByteArray(fis)).asJson().getBody().getObject();
            LOGGER.info(deployResponse);
        } catch (UnirestException | IOException e) {
            String msg = "Could not deploy framework on Marathon at " + marathonEndpoint + " => " + e.getMessage();
            LOGGER.error( msg );
            throw new MinimesosException( msg, e );
        }
    }
}
