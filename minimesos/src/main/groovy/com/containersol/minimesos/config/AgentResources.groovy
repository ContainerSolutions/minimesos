package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class AgentResources extends Block {

    static final ResourceDef DEFAULT_CPU = new ResourceDef("*", 1)
    static final ResourceDef DEFAULT_MEM = new ResourceDef("*", 4096)
    static final ResourceDef DEFAULT_DISK = new ResourceDef("*", 200)
    static final ResourceDef DEFAULT_PORTS = new ResourceDef("*", "[31000-32000]")

    HashMap<String, ResourceDef> cpus
    HashMap<String, ResourceDef> mems
    HashMap<String, ResourceDef> disks
    HashMap<String, ResourceDef> ports

    public AgentResources() {

        cpus = new HashMap<>()
        addResource(cpus, DEFAULT_CPU)

        mems = new HashMap<>()
        addResource(mems, DEFAULT_MEM)

        disks = new HashMap<>()
        addResource(disks, DEFAULT_DISK)

        ports = new HashMap<>()
        addResource(ports, DEFAULT_PORTS)

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

    static void addResource(HashMap<String, ResourceDef> resources, ResourceDef resource) {
        resources.put(resource.role, resource)
    }

}
