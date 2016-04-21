package com.containersol.minimesos.config

public class ContainerConfigBlock extends GroovyBlock implements ContainerConfig {

    String imageName
    String imageTag

    public ContainerConfigBlock() {

    }

    public ContainerConfigBlock(String name, String tag) {
        this.imageName = name
        this.imageTag = tag;
    }

}
