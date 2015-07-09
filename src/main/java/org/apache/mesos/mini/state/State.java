package org.apache.mesos.mini.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by peldan on 09/07/15.
 */
public class State {


    public static State fromJSON(JSONObject obj) {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.p
    }

    public ArrayList<Framework> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(ArrayList<Framework> frameworks) {
        this.frameworks = frameworks;
    }

    private ArrayList<Framework> frameworks = new ArrayList<>();
}
