package org.apache.mesos.mini.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class State {


    public static State fromJSON(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonString, State.class);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO throw something useful..
        }
    }

    public ArrayList<Framework> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(ArrayList<Framework> frameworks) {
        this.frameworks = frameworks;
    }

    private ArrayList<Framework> frameworks = new ArrayList<>();

    public Framework getFramework(String name) {
        for(Framework fw : getFrameworks()) {
            if (fw.getName().equals(name)) return fw;
        }
        return null;
    }
}
