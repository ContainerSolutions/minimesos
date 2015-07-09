package org.apache.mesos.mini.state;

/**
 * Created by peldan on 09/07/15.
 */
public class Task {

    private String name;
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
