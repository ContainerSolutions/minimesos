package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility for dealing with Mesos resources
 */
public class ResourceUtil {

    private ResourceUtil() {

    }

    /**
     * Turns a Mesos resource string into a List of ports.
     * <p>
     * Example: 'ports(*):[31000-32000],;cpus(*):0.2; mem(*):256; disk(*):200' returns [31000, 32000]
     *
     * @param mesosResourceString Mesos resource string
     * @return list of ports if any
     * @throws MinimesosException if resource string is incorrect
     */
    public static ArrayList<Integer> parsePorts(String mesosResourceString) {
        if (mesosResourceString == null) {
            throw new MinimesosException("Resource string is null");
        }
        String portRangeString = mesosResourceString.replaceAll(".*ports\\(.+\\):\\[(.*)\\].*", "$1");
        ArrayList<String> portRanges = new ArrayList<>(Arrays.asList(portRangeString.split(",")));
        ArrayList<Integer> returnList = new ArrayList<>();
        for (String portRange : portRanges) {

            if (!portRange.matches("\\d+-\\d+")) {
                throw new MinimesosException("Resource string '" + mesosResourceString + "' is incorrect. We only support a single port range.");
            }
            String[] ports = portRange.split("-");
            int startPort = Integer.valueOf(ports[0]);
            int endPort = Integer.valueOf(ports[1]);
            if (startPort > endPort) {
                throw new MinimesosException("Incorrect port range. Start port " + startPort + " is greater than end port " + endPort);
            }
            for (int i = startPort; i <= endPort; i++) {
                returnList.add(i);
            }
        }
        return returnList;
    }

}
