package com.containersol.minimesos.config

import org.junit.Test

import static org.junit.Assert.assertEquals;

public class ResourceDefScalarTest {

    @Test
    public void testDotParsing() {

        ResourceDefScalar resource = new ResourceDefScalar()
        resource.setValue("1.2");

        assertEquals(1.2, resource.getValue(), 0.0001)

    }

    @Test(expected = NumberFormatException.class)
    public void testCommaParsing() {

        ResourceDefScalar resource = new ResourceDefScalar()
        resource.setValue("1,2");

    }

}