package org.apache.mesos.mini.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

public class MesosClusterStateResponse implements Callable<Boolean> {
    private final Logger LOGGER = Logger.getLogger(MesosClusterStateResponse.class);
    private final String mesosMasterUrl;
    private final int expectedNumberOfSlaves;

    public MesosClusterStateResponse(String mesosMasterUrl, int expectedNumberOfSlaves) {
        this.mesosMasterUrl = mesosMasterUrl;
        this.expectedNumberOfSlaves = expectedNumberOfSlaves;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            int activated_slaves = Unirest.get("http://" + mesosMasterUrl + "/state.json").asJson().getBody().getObject().getInt("activated_slaves");
            if (!(activated_slaves == expectedNumberOfSlaves)) {
                LOGGER.info("Waiting for " + expectedNumberOfSlaves + " activated slaves - current number of activated slaves: " + activated_slaves);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.debug("Polling MesosMaster state on host: \"" + mesosMasterUrl + "\"...");
            return false;
        } catch (Exception e) {
            LOGGER.error("An error occured while polling mesos master", e);
            return false;
        }
        return true;
    }

    public void waitFor() {

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(this, is(true));

        LOGGER.info("MesosMaster state discovered successfully");
    }
}
