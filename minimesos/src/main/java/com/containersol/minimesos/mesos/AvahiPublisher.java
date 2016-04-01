package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes services to Avahi
 */
public class AvahiPublisher extends AbstractContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainer.class);

    public static final String IMAGE_NAME = "containersol/avahi";
    public static final String TAG = "latest";

    private final String ipAddress;
    private final String fqdn;

    public AvahiPublisher(String fqdn, String ipAddress) {
        this.ipAddress = ipAddress;
        this.fqdn = fqdn;
    }

    @Override
    public String getRole() {
        return "avahi-publisher";
    }

    @Override
    protected void pullImage() {
        pullImage(IMAGE_NAME, TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.build().createContainerCmd(IMAGE_NAME + ":" + TAG)
                .withPrivileged(true)
                .withBinds(Bind.parse("/var/run/dbus/system_bus_socket:/var/run/dbus/system_bus_socket"))
                .withName(getName())
                .withEnv("FQDN=" + fqdn, "IP=" + ipAddress);
    }

    public static void main(String[] args) {
        AvahiPublisher p = new AvahiPublisher("bla.local", "172.17.0.2");
        p.start(5);

        System.out.println(p);
    }

}
