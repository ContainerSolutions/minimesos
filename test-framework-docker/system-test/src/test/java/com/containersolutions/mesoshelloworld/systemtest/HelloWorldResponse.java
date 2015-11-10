package com.containersolutions.mesoshelloworld.systemtest;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Response which waits until endpoint is ready
 */
public class HelloWorldResponse {

    private boolean discoverySuccessful;

    public HelloWorldResponse(Set<String> ipAddresses, List<Integer> ports, long timeout) {
        await().atMost(timeout, TimeUnit.SECONDS).until(new TasksCall(ipAddresses, ports));
    }

    public boolean isDiscoverySuccessful() {
        return discoverySuccessful;
    }

    class TasksCall implements Callable<Boolean> {

        private final Set<String> ipAddresses;
        private final List<Integer> ports;

        public TasksCall(Set<String> ipAddresses, List<Integer> ports) {
            this.ipAddresses = ipAddresses;
            this.ports = ports;
        }

        @Override
        public Boolean call() throws Exception {

            final Set<String> goodHosts = new HashSet<>(ipAddresses.size());

            ipAddresses.forEach(ip -> {

                if( !goodHosts.contains(ip) ) {
                    ports.forEach(p -> {

                        if (!goodHosts.contains(ip)) {
                            String url = "http://" + ip + ":" + p;
                            try {
                                System.out.println( goodHosts.size() + ". " + url + " => " + Unirest.get(url).asString().getBody());
                                goodHosts.add( ip );
                            } catch (UnirestException e) {
                                // do nothing
                            }
                        }
                    });

                }

            });

            discoverySuccessful = (goodHosts.size() == ipAddresses.size());
            return discoverySuccessful;

        }

    }
}
