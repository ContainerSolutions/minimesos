package com.containersol.minimesos.docker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class DockerClientFactoryTest {
    String dummyCertPath;
    @Before
    public void createDummyCert() throws IOException {
        File crt = new File("/tmp/" + String.valueOf(Math.round(Math.random())));
        crt.createNewFile();
        dummyCertPath = crt.getAbsolutePath();
    }

    @After
    public void removeDummyCert() {
        new File(dummyCertPath).delete();
    }

    @Test
    public void testSetFromProperty() throws IOException {
        System.setProperty("docker.io.url", "tcp://notreal:2387");
        System.setProperty("docker.io.dockerCertPath", dummyCertPath);
        DockerClientFactory.build();
        assertEquals(DockerClientFactory.getDockerUri(), "tcp://notreal:2387");
        assertEquals(DockerClientFactory.getDockerCertPath(), dummyCertPath);
    }

    @Test
    public void testSetOnlyExistingCertFile() {
        System.setProperty("docker.io.dockerCertPath", "/obviously/fake");
        DockerClientFactory.build();
        assertEquals(DockerClientFactory.getDockerCertPath(), null);
    }
}