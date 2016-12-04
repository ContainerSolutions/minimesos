package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Slf4j
class AgentResourcesConfig extends GroovyBlock {

    static public final ResourceDefScalar DEFAULT_CPU = new ResourceDefScalar("*", 4)
    static public final ResourceDefScalar DEFAULT_MEM = new ResourceDefScalar("*", 512)
    static public final ResourceDefScalar DEFAULT_DISK = new ResourceDefScalar("*", 2000)
    static public final ResourceDefRanges DEFAULT_PORTS = new ResourceDefRanges("*", "[31000-32000]")

    HashMap<String, ResourceDefScalar> cpus
    HashMap<String, ResourceDefScalar> mems
    HashMap<String, ResourceDefScalar> disks
    HashMap<String, ResourceDefRanges> ports

    public AgentResourcesConfig() {
        this(true)
    }

    private AgentResourcesConfig(boolean defaults) {
        cpus = new HashMap<>()
        mems = new HashMap<>()
        disks = new HashMap<>()
        ports = new HashMap<>()
        if (defaults) {
            setDefaults()
        }
    }

    private void setDefaults() {
        addResource(cpus, DEFAULT_CPU)
        addResource(mems, DEFAULT_MEM)
        addResource(disks, DEFAULT_DISK)
        addResource(ports, DEFAULT_PORTS)
    }

    /**
     * Generates resources object from Mesos string definition
     * @param strResources in format like ports(*):[8081-8082]; cpus(*):1.2
     * @return
     */
    static AgentResourcesConfig fromString(String strResources) {

        String pattern = "(\\w+)\\(([A-Za-z0-9_\\*]+)\\):(\\[?[0-9_\\-\\*., ]+\\]?)"
        AgentResourcesConfig resources = new AgentResourcesConfig(false)

        String[] split = strResources.split(";")
        for (String str : split) {
            Matcher matcher = str.trim() =~ pattern
            if (matcher.matches() && (matcher.groupCount() == 3)) {
                String type = matcher.group(1)
                String role = matcher.group(2)
                String value = matcher.group(3)

                switch (type) {
                    case "ports":
                        resources.ports.put(role, new ResourceDefRanges(role, value))
                        break
                    case "cpus":
                        resources.cpus.put(role, new ResourceDefScalar(role, Double.valueOf(value)))
                        break
                    case "mem":
                        resources.mems.put(role, new ResourceDefScalar(role, Double.valueOf(value)))
                        break
                    case "disk":
                        resources.disks.put(role, new ResourceDefScalar(role, Double.valueOf(value)))
                        break
                }

            }
        }

        return resources
    }

    def cpu(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(cpus, loadResourceDef(cl, ResourceDefScalar.class))
    }

    def mem(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(mems, loadResourceDef(cl, ResourceDefScalar.class))
    }

    def disk(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(disks, loadResourceDef(cl, ResourceDefScalar.class))
    }

    def ports(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(ports, loadResourceDef(cl, ResourceDefRanges.class))
    }

    ResourceDef loadResourceDef(Closure cl, Class<ResourceDef> resourceDefClass) {
        ResourceDef resource = resourceDefClass.newInstance()
        delegateTo(resource, cl)
        return resource
    }

    /**
     *
     * @return formatted string with definition of resources. Example: ports(*):[31000-32000]; cpus(*):0.2; mem(*):256; disk(*):200
     */
    String asMesosString() {

        StringBuilder builder = new StringBuilder()

        for (ResourceDef resourceDef : ports.values()) {
            appendResource(builder, resourceDef, "ports")
        }
        for (ResourceDef resourceDef : cpus.values()) {
            appendResource(builder, resourceDef, "cpus")
        }
        for (ResourceDef resourceDef : mems.values()) {
            appendResource(builder, resourceDef, "mem")
        }
        for (ResourceDef resourceDef : disks.values()) {
            appendResource(builder, resourceDef, "disk")
        }

        return builder.toString()

    }

    static void appendResource(StringBuilder builder, ResourceDef resourceDef, String res) {
        if (builder.length() > 0) {
            builder.append("; ")
        }
        builder.append(res).append("(").append(resourceDef.role).append("):").append(resourceDef.valueAsString());
    }

    static void addResource(HashMap<String, ResourceDef> resources, ResourceDef resource) {
        resources.put(resource.role, resource)
    }

}
