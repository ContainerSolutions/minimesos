package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class CollectionsUtilsTest {

    @Test(expected = MinimesosException.class)
    public void testSplitCmd_uncoherentCommandLine() {
        CollectionsUtils.splitCmd("foo bar='test");
    }

    @Test
    public void testSplitCmd_cmdLineEmpty() {
        assertArrayEquals(
            CollectionsUtils.splitCmd(""),
            new String[]{}
        );
    }

    @Test
    public void testSplitCmd_cmdLineNoQuotes() {
        assertArrayEquals(
            CollectionsUtils.splitCmd("foo bar baaz qux"),
            new String[]{"foo", "bar", "baaz", "qux"}
        );
    }

    @Test
    public void testSplitCmd_cmdLineWithQuotes() {
        assertArrayEquals(
            CollectionsUtils.splitCmd("foo='bar baaz' qux"),
            new String[]{"foo='bar baaz'", "qux"}
        );
    }

    @Test
    public void testSplitCmd_cmdLineWithDoubleQuotes() {
        assertArrayEquals(
            CollectionsUtils.splitCmd("foo=\"bar baaz\" qux"),
            new String[]{"foo=\"bar baaz\"", "qux"}
        );
    }
}
