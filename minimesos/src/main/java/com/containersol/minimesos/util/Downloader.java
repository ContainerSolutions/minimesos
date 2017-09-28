package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpStatus;

import java.net.URI;

public class Downloader {

    public String getFileContentAsString(String url) throws MinimesosException {
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url)
                .header("content-type", "*/*")
                .asString();
        } catch (UnirestException e) {
            throw new MinimesosException(String.format("Cannot fetch file '%s': '%s'", url, e.getMessage()));
        }
        if (response.getStatus() != HttpStatus.SC_OK) {
            throw new MinimesosException(String.format("Cannot fetch file '%s': '%s'", url, response.getStatus()));
        }
        return response.getBody();
    }
}
