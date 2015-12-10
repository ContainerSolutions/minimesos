package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MinimesosException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;

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

    public boolean deployTask(String taskJson) {

        if( marathonIp == null ) {
            return false;
        }

        String marathonEndpoint = getMarathonEndpoint();
        try {

            byte[] task = taskJson.getBytes(Charset.forName("UTF-8"));

            HttpResponse<JsonNode> response = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(task).asJson();
            JSONObject deployResponse = response.getBody().getObject();

            if( response.getStatus() == HttpStatus.SC_CREATED ) {
                LOGGER.info(deployResponse);
            } else {
                throw new MinimesosException("Marathon did not accept the task: " + deployResponse);
            }

        } catch (UnirestException e) {
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
