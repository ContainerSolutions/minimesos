package com.containersol.minimesos.junit;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.main.factory.MesosClusterContainersFactory;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit Rule wrapper of Mesos Cluster
 */
public class MesosClusterResource extends MesosCluster implements TestRule {

    public MesosClusterResource(ClusterArchitecture clusterArchitecture) {
        super(clusterArchitecture.getClusterConfig(), clusterArchitecture.getClusterContainers().getContainers());
    }

    public Statement apply(Statement base, Description description) {
        return statement(base);
    }

    private Statement statement(final Statement base) {
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

    protected void before() throws Throwable {
        start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                MesosClusterFactory factory = new MesosClusterContainersFactory();
                factory.destroyRunningCluster(getClusterId());
            }
        });
    }

    protected void after() {
        stop();
    }

    public void destroy() {
        MesosClusterFactory factory = new MesosClusterContainersFactory();
        destroy(factory);
    }
}
