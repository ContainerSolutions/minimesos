package org.apache.mesos.mini.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.mesos.mini.MesosCluster;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

/**
* Created by peldan on 08/07/15.
*/
public class MesosClusterStateResponse implements Callable<Boolean> {


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
                MesosCluster.LOGGER.info("Waiting for " + expectedNumberOfSlaves + " activated slaves - current number of activated slaves: " + activated_slaves);
                return false;
            }
        } catch (UnirestException e) {
            MesosCluster.LOGGER.info("Polling MesosMaster state on host: \"" + mesosMasterUrl + "\"...");
            return false;
        } catch (Exception e) {
            MesosCluster.LOGGER.error("An error occured while polling mesos master", e);
            return false;
        }
        return true;
    }

    public void waitFor() {

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(this, is(true));

        MesosCluster.LOGGER.info("MesosMaster state discovered successfully");
    }
}
