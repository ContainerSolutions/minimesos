package com.containersol.minimesos.docker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class DockerClientFactoryTest {
    String dummyCertPath;
    @Before
    public void before() throws IOException {
        File crt = new File("/tmp/" + String.valueOf(Math.round(Math.random()*100)));
        crt.createNewFile();
        dummyCertPath = crt.getAbsolutePath();
    }

    @After
    public void after() {
        new File(dummyCertPath).delete();
        DockerClientFactory.build(System.getProperties());
        DockerClientFactory.build(System.getenv());
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

    @Test
    public void envTakesPrecedence() {
        Properties props = new Properties();
        Map<String,String> env = new TreeMap<>();
        env.put("DOCKER_HOST", "tcp://notreal.env");
        env.put("DOCKER_CERT_PATH", dummyCertPath);
        props.setProperty("docker.io.url", "tcp://notreal.props");
        props.setProperty("docker.io.dockerCertPath", "/tmp");

        DockerClientFactory.build(env);
        DockerClientFactory.build(props);

        assertEquals(DockerClientFactory.getDockerUri(), "tcp://notreal.env");
        assertEquals(DockerClientFactory.getDockerCertPath(), dummyCertPath);
    }
}