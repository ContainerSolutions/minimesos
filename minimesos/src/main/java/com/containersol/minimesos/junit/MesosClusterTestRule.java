package com.containersol.minimesos.junit;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.main.factory.MesosClusterContainersFactory;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit Rule extension of Mesos Cluster to use in JUnit.
 *
 * TODO: see https://github.com/ContainerSolutions/minimesos/issues/8 for completion
 */
public class MesosClusterTestRule extends MesosCluster implements TestRule {

    public MesosClusterTestRule(ClusterArchitecture clusterArchitecture) {
        super(clusterArchitecture.getClusterConfig(), clusterArchitecture.getClusterContainers().getContainers());
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
        start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                MesosClusterFactory factory = new MesosClusterContainersFactory();
                factory.destroyRunningCluster(getClusterId());
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
        MesosClusterFactory factory = new MesosClusterContainersFactory();
        destroy(factory);
    }

}
