package com.containersol.minimesos.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.array;
import static org.hamcrest.Matchers.is;

public class EnvironmentBuilderTest {

    @Test
    public void mergingSeveralSourcesProducesCorrectMap() {
        Map<String, String> source1 = new TreeMap<>();
        source1.put("envVar1", "value1");
        source1.put("envVar2", "value2");
        source1.put("envVar3", "value3");
        source1.put("envVar4", "value4");
        Map<String, String> source2 = new TreeMap<>();
        source2.put("envVar5", "value5");
        source2.put("envVar6", "value6");

        String[] result = EnvironmentBuilder.newEnvironment()
                .withValues(source1)
                .withValue("envVarX", "valueX")
                .withValues(source2)
                .createEnvironment();

        Assert.assertThat(result, array(is("envVar1=value1"),
                is("envVar2=value2"), is("envVar3=value3"), is("envVar4=value4"),
                is("envVar5=value5"), is("envVar6=value6"), is("envVarX=valueX")));
    }
}