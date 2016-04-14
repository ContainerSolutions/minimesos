package com.containersol.minimesos.junit;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * JUnit Rule extension of Mesos Cluster to use in JUnit.
 *
 * TODO: see https://github.com/ContainerSolutions/minimesos/issues/8 for completion
 */
public class MesosClusterTestRule implements TestRule {

    private MesosClusterFactory factory = new MesosClusterContainersFactory();

    private MesosCluster mesosCluster;

    public static MesosClusterTestRule fromFile(String minimesosFilePath) {
        try {
            MesosCluster cluster = new MesosClusterContainersFactory().createMesosCluster(new FileInputStream(minimesosFilePath));
            return new MesosClusterTestRule(cluster);
        } catch (FileNotFoundException e) {
            throw new MinimesosException("Could not read minimesosFile at " + minimesosFilePath);
        }
    }

    private MesosClusterTestRule(MesosCluster mesosCluster) {
        this.mesosCluster = mesosCluster;
    }

    /**
     * Modifies the method-running {@link Statement} to implement this test-running rule.
     *
     * @param base        The {@link Statement} to be modified
     * @param description A {@link Description} of the test implemented in {@code base}
     * @return a new statement, which may be the same as {@code base}, a wrapper around {@code base}, or a completely new Statement.
     */
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    /**
     * Execute before the test
     */
    protected void before() {
        mesosCluster.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                factory.destroyRunningCluster(mesosCluster.getClusterId());
            }
        });
    }

    /**
     * Execute after the test
     */
    protected void after() {
        stop();
    }

    /**
     * Destroys cluster using docker based factory of cluster members
     */
    public void stop() {
        mesosCluster.destroy(factory);
    }

    public MesosCluster getMesosCluster() {
        return mesosCluster;
    }

    public MesosClusterFactory getFactory() {
        return factory;
    }
}
