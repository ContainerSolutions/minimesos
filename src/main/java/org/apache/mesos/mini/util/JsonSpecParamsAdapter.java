package org.apache.mesos.mini.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aleks on 26/08/15.
 */
class JsonSpecParamsAdapter extends TypeAdapter<JsonContainerWithSpec> {
    @Override
    public void write(JsonWriter out, JsonContainerWithSpec value) throws IOException {

    }

    @Override
    public JsonContainerWithSpec read(JsonReader jsonReader) throws IOException {
        JsonContainerWithSpec wspec = new JsonContainerWithSpec();
        if (jsonReader.peek().equals(JsonToken.BEGIN_OBJECT)) {
            jsonReader.beginObject();
            while (!jsonReader.peek().equals(JsonToken.END_OBJECT)) {
                if (jsonReader.peek().equals(JsonToken.NAME)) {
                    if (jsonReader.nextName().equals("expose")) {
                        JsonSpecParamExpose ep = new JsonSpecParamExpose();
                        List<Integer> exposeList = new ArrayList<>();
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            exposeList.add(jsonReader.nextInt());
                        }
                        jsonReader.endArray();
                        wspec.setExpose(ep.setParameter(exposeList.toArray(new Integer[0])));
                    }

                    if (jsonReader.nextName().equals("name")) {
                        wspec.name = jsonReader.nextString();
                    }
                    if (jsonReader.nextName().equals("cmd")) {
                        jsonReader.beginArray();
                        List<String> cmdList = new ArrayList<>();
                        while (jsonReader.hasNext()) {
                            cmdList.add(jsonReader.nextString());
                        }
                        jsonReader.endArray();
                        wspec.setCmd(new JsonSpecParamCmd().setParameter(cmdList.toArray(new String[0])));
                    }
                    if (jsonReader.nextName().equals("volumes")) {
                        jsonReader.beginArray();
                        List<String> volumesList = new ArrayList<>();
                        while (jsonReader.hasNext()) {
                            volumesList.add(jsonReader.nextString());
                        }
                        jsonReader.endArray();
                        wspec.setVolumes(new JsonSpecParamVolumes().setParameter(volumesList.toArray(new String[0])));
                    }
                    if (jsonReader.nextName().equals("volumes_from")) {
                        jsonReader.beginArray();
                        List<String> volumesFromList = new ArrayList<>();
                        while (jsonReader.hasNext()) {
                            volumesFromList.add(jsonReader.nextString());
                        }
                        jsonReader.endArray();
                        wspec.setVolumes_from(new JsonSpecParamVolumesFrom().setParameter(volumesFromList.toArray(new String[0])));
                    }
                    if (jsonReader.nextName().equals("links")) {
                        jsonReader.beginArray();
                        List<String> linksList = new ArrayList<>();
                        while (jsonReader.hasNext()) {
                            linksList.add(jsonReader.nextString());
                        }
                        jsonReader.endArray();
                        wspec.setLinks(new JsonSpecParamLinks().setParameter(linksList.toArray(new String[0])));
                    }
                    if (jsonReader.nextName().equals("environment")) {
                        jsonReader.beginArray();
                        List<String> envList = new ArrayList<>();
                        while (jsonReader.hasNext()) {
                            envList.add(jsonReader.nextString());
                        }
                        jsonReader.endArray();
                        wspec.setEnvironment(new JsonSpecParamEnv().setParameter(envList.toArray(new String[0])));
                    }
                }
            }
            jsonReader.endObject();
        }
        return wspec;
    }

}
