package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosDns;
import com.containersol.minimesos.config.MesosDNSConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;

import java.util.HashMap;
import java.util.Map;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;

/**
 * Mesos DNS automatically registers and deregisters Mesos services.
 */
public class MesosDnsContainer extends AbstractContainer implements MesosDns {

    private static final String DNS_PORT = "5353";

    private static final String DOMAIN = "mm";

    private static final String REFRESH_SECONDS = "1";

    private MesosDNSConfig config;

    public MesosDnsContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MesosDNSConfig());
    }

    private MesosDnsContainer(MesosCluster cluster, String uuid, String containerId, MesosDNSConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    public MesosDnsContainer(MesosDNSConfig mesosDNS) {
        super(mesosDNS);
        this.config = mesosDNS;
    }

    @Override
    public String getRole() {
        return "mesosdns";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.build()
            .createContainerCmd(config.getImageName() + ":" + config.getImageTag())
            .withEnv(newEnvironment()
                .withValues(getMesosDNSEnvVars())
                .createEnvironment())
            .withCmd("-v=2", "-config=/etc/mesos-dns/config.json")
            .withExposedPorts(new ExposedPort(Integer.valueOf(DNS_PORT), InternetProtocol.UDP))
            .withName(getName());
    }

    @Override
    protected int getServicePort() {
        return Integer.valueOf(DNS_PORT);
    }

    private Map<String,String> getMesosDNSEnvVars() {
        Map<String, String> mesosDNSEnvVars = new HashMap<>();
        mesosDNSEnvVars.put("MESOS_DNS_ZK", getCluster().getZooKeeper().getFormattedZKAddress() + "/mesos");
        mesosDNSEnvVars.put("MESOS_DNS_DOMAIN", DOMAIN);
        mesosDNSEnvVars.put("MESOS_DNS_PORT", DNS_PORT);
        mesosDNSEnvVars.put("MESOS_DNS_REFRESH_SECONDS", REFRESH_SECONDS);
        return mesosDNSEnvVars;
    }
}
