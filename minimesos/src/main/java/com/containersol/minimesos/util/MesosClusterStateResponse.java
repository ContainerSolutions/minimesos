package com.containersol.minimesos.util;

import com.containersol.minimesos.MesosCluster;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

public class MesosClusterStateResponse implements Callable<Boolean> {

    private final Logger LOGGER = Logger.getLogger(MesosClusterStateResponse.class);

    private final MesosCluster mesosCluster;

    public MesosClusterStateResponse(MesosCluster mesosCluster) {
        this.mesosCluster = mesosCluster;
    }

    @Override
    public Boolean call() throws Exception {
        String stateUrl = mesosCluster.getMesosMasterContainer().getStateUrl();
        try {
            int activated_slaves = Unirest.get(stateUrl).asJson().getBody().getObject().getInt("activated_slaves");
            if (activated_slaves != mesosCluster.getSlaves().length) {
                LOGGER.debug("Waiting for " + mesosCluster.getSlaves().length + " activated slaves - current number of activated slaves: " + activated_slaves);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.debug("Polling Mesos Master state on host: \"" + stateUrl + "\"...");
            return false;
        } catch (Exception e) {
            LOGGER.error("An error occured while polling Mesos master", e);
            return false;
        }

        return true;
    }

    public void waitFor() {
        await()
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(this);

        LOGGER.debug("MesosMaster state discovered successfully");
    }
}
