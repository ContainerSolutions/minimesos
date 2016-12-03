package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosDNS;
import com.containersol.minimesos.config.MesosDNSConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;

import java.util.HashMap;
import java.util.Map;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;

/**
 * Mesos DNS automatically registers and deregisters Mesos services.
 */
public class MesosDNSContainer extends AbstractContainer implements MesosDNS {

    private static final String DNS_PORT = "5353";

    private static final String DOMAIN = "mm";

    private MesosDNSConfig config;

    public MesosDNSContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MesosDNSConfig());
    }

    private MesosDNSContainer(MesosCluster cluster, String uuid, String containerId, MesosDNSConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    public MesosDNSContainer(MesosDNSConfig mesosDNS) {
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
            .withName(getName());
    }

    private Map<String,String> getMesosDNSEnvVars() {
        Map<String, String> mesosDNSEnvVars = new HashMap<>();
        mesosDNSEnvVars.put("MESOS_DNS_ZK", getCluster().getZooKeeper().getFormattedZKAddress() + "/mesos");
        mesosDNSEnvVars.put("MESOS_DNS_DOMAIN", DOMAIN);
        mesosDNSEnvVars.put("MESOS_DNS_PORT", DNS_PORT);
        return mesosDNSEnvVars;
    }
}
