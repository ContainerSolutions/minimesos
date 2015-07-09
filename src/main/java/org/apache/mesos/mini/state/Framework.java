package org.apache.mesos.mini.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Framework {

    private String name;
    private ArrayList<Task> tasks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }
}
