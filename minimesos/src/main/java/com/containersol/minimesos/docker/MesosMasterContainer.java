package com.containersol.minimesos.docker;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosDns;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.util.Environment;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;
import static com.jayway.awaitility.Awaitility.await;

/**
 * Mesos Master adds the "server" component for Apache Mesos
 */
public class MesosMasterContainer extends MesosContainerImpl implements MesosMaster {

    public MesosMasterContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MesosMasterConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
    }

    private MesosMasterContainer(MesosCluster cluster, String uuid, String containerId, MesosMasterConfig config) {
        super(cluster, uuid, containerId, config);
    }

    public MesosMasterContainer(MesosMasterConfig master) {
        super(master);
    }

    @Override
    public int getServicePort() {
        return MesosMasterConfig.MESOS_MASTER_PORT;
    }

    @Override
    public URI getServiceUrl() {
        URI serviceUri = null;

        String protocol = getServiceProtocol();

        String host;
        if (Environment.isRunningInJvmOnMacOsX()) {
            host = "localhost";
        } else {
            host = getIpAddress();
        }

        int port = getServicePort();
        String path = getServicePath();

        if (StringUtils.isNotEmpty(host)) {
            try {
                serviceUri = new URI(protocol, null, host, port, path, null, null);
            } catch (URISyntaxException e) {
                throw new MinimesosException("Failed to form service URL for " + getName(), e);
            }
        }

        return serviceUri;
    }


    protected Map<String, String> getMesosMasterEnvVars() {
        Map<String, String> envs = new TreeMap<>();
        envs.put("MESOS_QUORUM", "1");
        if (((MesosMasterConfig) config).getAuthenticate() && ((MesosMasterConfig) config).getAclJson() != null) {
            envs.put("MESOS_AUTHENTICATE", String.valueOf(((MesosMasterConfig) config).getAuthenticate()));
            envs.put("MESOS_ACLS", ((MesosMasterConfig) config).getAclJson());
        }
        envs.put("MESOS_ZK", getFormattedZKAddress());
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        if (getCluster() != null) {
            envs.put("MESOS_CLUSTER", getCluster().getClusterName());
        }
        return envs;
    }

    @Override
    public String getRole() {
        return "master";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        int port = getServicePort();
        ExposedPort exposedPort = ExposedPort.tcp(port);

        Ports portBindings = new Ports();
        if (getCluster().isMapPortsToHost()) {
            portBindings.bind(exposedPort, Ports.Binding.bindPort(port));
        }

        CreateContainerCmd cmd = DockerClientFactory.build().createContainerCmd(getImageName() + ":" + getImageTag())
            .withName(getName())
            .withExposedPorts(new ExposedPort(getServicePort()))
            .withEnv(newEnvironment()
                .withValues(getMesosMasterEnvVars())
                .withValues(getSharedEnvVars())
                .createEnvironment())
            .withPortBindings(portBindings);

        MesosDns mesosDns = getCluster().getMesosDns();
        if (mesosDns != null) {
            cmd.withDns(mesosDns.getIpAddress());
        }

        return cmd;
    }

    @Override
    public void waitFor() {
        new MesosMasterContainer.MesosClusterStateResponse(getCluster()).waitFor();
    }

    public static class MesosClusterStateResponse implements Callable<Boolean> {

        private static final Logger LOGGER = LoggerFactory.getLogger(MesosClusterStateResponse.class);

        private final MesosCluster mesosCluster;

        public MesosClusterStateResponse(MesosCluster mesosCluster) {
            this.mesosCluster = mesosCluster;
        }

        @Override
        public Boolean call() throws Exception {
            String stateUrl = mesosCluster.getMaster().getStateUrl();
            try {
                int activatedAgents = Unirest.get(stateUrl).asJson().getBody().getObject().getInt("activated_slaves");
                if (activatedAgents != mesosCluster.getAgents().size()) {
                    LOGGER.debug("Waiting for " + mesosCluster.getAgents().size() + " activated agents - current number of activated agents: " + activatedAgents);
                    return false;
                }
            } catch (UnirestException e) { //NOSONAR
                // in case of error just return false
                LOGGER.debug("Polling Mesos Master state on host: \"" + stateUrl + "\"...");
                return false;
            } catch (Exception e) { //NOSONAR
                // in case of error just return false
                LOGGER.error("An error occured while polling Mesos master", e);
                return false;
            }

            return true;
        }

        public void waitFor() {
            await("Waiting until Mesos master state endpoint is available")
                    .atMost(mesosCluster.getClusterConfig().getTimeout(), TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(this);

            LOGGER.debug("MesosMaster state discovered successfully");
        }
    }
}
