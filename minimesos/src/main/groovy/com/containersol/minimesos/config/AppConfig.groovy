package com.containersol.minimesos.config

/**
 * Configuration for a Marathon app. Path is relative to the minimesosFile.
 */
class AppConfig {

    private String marathonJson

    public void setMarathonJson(String marathonJson) {
        this.marathonJson = marathonJson
    }

    public String getMarathonJson() {
        return marathonJson
    }

    /**
     * @return URI, if <code>marathonJson</code> represents an absolute URI; otherwise - null
     */
    public URI asAbsoluteUri() {

        URI uri = null

        if (marathonJson != null) {
            try {
                uri = URI.create(marathonJson)
                if (!uri.isAbsolute()) {
                    uri = null
                }
            } catch (IllegalArgumentException ignored) {
                // means this is not a valid URI
            }
        }

        return uri

    }

}
