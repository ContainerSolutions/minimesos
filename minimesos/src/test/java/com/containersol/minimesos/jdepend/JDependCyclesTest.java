package com.containersol.minimesos.jdepend;

import jdepend.framework.JDepend;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Ensures absence of dependency cycles
 */
public class JDependCyclesTest {

    private static final String EXPECTED_PACKAGE = "com.containersol.minimesos";

    private JDepend jdepend;

    @Before
    public void before() throws IOException {
        jdepend = new JDepend();
        jdepend.addDirectory("build/classes/main");
    }

    /**
     * Tests that a package dependency cycle does not
     * exist for any of the analyzed packages.
     */
    @Test
    public void testAllPackages() {

        jdepend.analyze();
        assertTrue("Something is wrong with JDepend setup", jdepend.getPackages().size() > 0);
        assertNotNull("Package " + EXPECTED_PACKAGE + " is not found. Please, check", jdepend.getPackage(EXPECTED_PACKAGE));
        assertEquals("Dependency Cycles are introduced", false, jdepend.containsCycles());

    }

}
