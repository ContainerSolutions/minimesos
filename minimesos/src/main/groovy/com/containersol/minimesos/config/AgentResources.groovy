package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class AgentResources extends GroovyBlock {

    static public final ResourceDef DEFAULT_CPU = new ResourceDef("*", (double) 0.2)
    static public final ResourceDef DEFAULT_MEM = new ResourceDef("*", 256)
    static public final ResourceDef DEFAULT_DISK = new ResourceDef("*", 200)
    static public final ResourceDef DEFAULT_PORTS = new ResourceDef("*", "[31000-32000]")

    HashMap<String, ResourceDef> cpus
    HashMap<String, ResourceDef> mems
    HashMap<String, ResourceDef> disks
    HashMap<String, ResourceDef> ports

    public AgentResources() {
        this(true)
    }

    private AgentResources(boolean defaults) {
        cpus = new HashMap<>()
        mems = new HashMap<>()
        disks = new HashMap<>()
        ports = new HashMap<>()
        if( defaults ) {
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
    static AgentResources fromString(String strResources) {

        String pattern = "(\\w+)\\(([A-Za-z0-9_\\*]+)\\):(\\[?[0-9_\\-\\*.]+\\]?)"
        AgentResources resources = new AgentResources(false)

        String[] split = strResources.split(";")
        for( String str : split ) {
            Matcher matcher = str.trim() =~ pattern
            if( matcher.matches() && (matcher.groupCount() == 3) ) {
                String type = matcher.group(1)
                String role = matcher.group(2)
                String value = matcher.group(3)

                switch (type) {
                    case "ports":
                        resources.ports.put(role, new ResourceDef(role, value))
                        break
                    case "cpus":
                        resources.cpus.put(role, new ResourceDef(role, Double.valueOf(value)))
                        break
                    case "mem":
                        resources.mems.put(role, new ResourceDef(role, Integer.valueOf(value)))
                        break
                    case "disk":
                        resources.disks.put(role, new ResourceDef(role, Integer.valueOf(value)))
                        break
                }

            }
        }

        return resources
    }

    def cpu(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(cpus, loadResourceDef(cl))
    }

    def mem(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(mems, loadResourceDef(cl))
    }

    def disk(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(disks, loadResourceDef(cl))
    }
    def ports(@DelegatesTo(ResourceDef) Closure cl) {
        addResource(ports, loadResourceDef(cl))
    }

    ResourceDef loadResourceDef(Closure cl) {
        ResourceDef resource = new ResourceDef()
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
        builder.append(res).append("(").append(resourceDef.role).append("):").append(resourceDef.value);
    }

    static void addResource(HashMap<String, ResourceDef> resources, ResourceDef resource) {
        resources.put(resource.role, resource)
    }

}
