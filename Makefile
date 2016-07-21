default: build
.PHONY: setup deps test build

setup:
	sudo route delete 172.17.0.0/16; sudo route -n add 172.17.0.0/16 $(shell docker-machine ip ${shell DOCKER_MACHINE_NAME})

deps:
	docker pull containersol/mesos-agent:0.25.0-0.1.0
	docker pull containersol/mesos-master:0.25.0-0.1.0
	docker pull gliderlabs/registrator:v6
	docker pull containersol/consul-server:0.6-1
	docker pull mesosphere/marathon:v0.15.3
	docker pull jplock/zookeeper:3.4.6
	docker pull containersol/alpine3.3-java8-jre:v1
	docker pull tutum/hello-world:latest

clean:
	./gradlew clean
	-docker rmi containersol/minimesos-cli:latest

build:
	./gradlew build --info --stacktrace

build-no-tests:
	./gradlew build --info --stacktrace -x test

test:
	./gradlew test --info --stacktrace

