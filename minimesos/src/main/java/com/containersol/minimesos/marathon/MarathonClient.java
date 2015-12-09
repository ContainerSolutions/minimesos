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

/**
 * Client to talk to Marathon's API
 */
public class MarathonClient {

    private static Logger LOGGER = Logger.getLogger(MarathonClient.class);

    private final String marathonIp;

    public MarathonClient( String marathonIp ) {
        this.marathonIp = marathonIp;
    }

    /**
     * Kill all apps that are currently running.
     */
    public void killAllApps() {
        if( marathonIp != null ) {
            String marathonEndpoint = getMarathonEndpoint();
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

    public String getMarathonEndpoint() {
        return "http://" + marathonIp + ":" + Marathon.MARATHON_PORT;
    }

    public boolean deployFramework(File marathonFile) {

        if( marathonIp == null ) {
            return false;
        }

        String marathonEndpoint = getMarathonEndpoint();
        JSONObject deployResponse;
        try (FileInputStream fis = new FileInputStream(marathonFile)) {
            deployResponse = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(IOUtils.toByteArray(fis)).asJson().getBody().getObject();
            LOGGER.info(deployResponse);
        } catch (UnirestException | IOException e) {
            String msg = "Could not deploy framework on Marathon at " + marathonEndpoint + " => " + e.getMessage();
            LOGGER.error( msg );
            throw new MinimesosException( msg, e );
        }

        return true;

    }

    public boolean isReady() {

        JSONObject appsResponse;
        try {
            appsResponse = Unirest.get( getMarathonEndpoint() + "/v2/apps").header("accept", "application/json").asJson().getBody().getObject();
            if (appsResponse.length() == 0) {
                return false;
            }
        } catch (UnirestException e) {
            return false;
        }

        return true;

    }

}
