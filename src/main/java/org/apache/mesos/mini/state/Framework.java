package org.apache.mesos.mini.state;

import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
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
