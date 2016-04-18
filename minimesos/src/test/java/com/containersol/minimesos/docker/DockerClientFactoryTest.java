package com.containersol.minimesos.docker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

public class DockerClientFactoryTest {
    String dummyCertPath;
    @Before
    public void before() throws IOException {
        File crt = new File("/tmp/" + String.valueOf(Math.round(Math.random())));
        crt.createNewFile();
        dummyCertPath = crt.getAbsolutePath();
    }

    @After
    public void after() {
        new File(dummyCertPath).delete();
        DockerClientFactory.build(System.getProperties());
    }

    @Test
    public void testSetFromProperty() throws IOException {
        Properties props = new Properties();
        props.setProperty("docker.io.url", "tcp://notreal:2387");
        props.setProperty("docker.io.dockerCertPath", dummyCertPath);
        DockerClientFactory.build(props);
        assertEquals(DockerClientFactory.getDockerUri(), "tcp://notreal:2387");
        assertEquals(DockerClientFactory.getDockerCertPath(), dummyCertPath);
    }

    @Test
    public void testSetOnlyExistingCertFile() {
        Properties props = new Properties();
        props.setProperty("docker.io.dockerCertPath", "/obviously/does/not/exist");
        DockerClientFactory.build(props);
        assertEquals(DockerClientFactory.getDockerCertPath(), null);
    }
}