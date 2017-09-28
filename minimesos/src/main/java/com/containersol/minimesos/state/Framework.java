package com.containersol.minimesos.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Framework {

    private boolean active;

    private boolean checkpoint;

    @JsonProperty("failover_timeout")
    private int failoverTimeout;

    private String hostname;

    private String id;

    private String name;

    private String role;

    private ArrayList<Task> tasks;
    private ArrayList<Executor> executors = new ArrayList<>();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(boolean checkpoint) {
        this.checkpoint = checkpoint;
    }

    public int getFailoverTimeout() {
        return failoverTimeout;
    }

    public void setFailoverTimeout(int failoverTimeout) {
        this.failoverTimeout = failoverTimeout;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    public ArrayList<Executor> getExecutors() {
        return executors;
    }

    public void setExecutors(ArrayList<Executor> executors) {
        this.executors = executors;
    }
}
