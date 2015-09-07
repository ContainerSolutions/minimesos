package org.apache.mesos.mini.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Framework {

    private boolean active;
    private boolean checkpoint;
    private int failover_timeout;
    private String hostname;
    private String id;
    private String name;
    private String role;
    private ArrayList<Task> tasks;

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
        return failover_timeout;
    }

    public void setFailoverTimeout(int failoverTimeout) {
        this.failover_timeout = failoverTimeout;
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
}
