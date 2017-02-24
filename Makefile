default: build
.PHONY: setup deps test build

setup:
	sudo route delete 172.17.0.0/16; sudo route -n add 172.17.0.0/16 $(shell docker-machine ip ${shell DOCKER_MACHINE_NAME})

deps:
	docker pull containersol/mesos-agent:1.0.0-0.1.0
	docker pull containersol/mesos-master:1.0.0-0.1.0
	docker pull gliderlabs/registrator:v6
	docker pull consul:0.7.1
	docker pull xebia/mesos-dns:v0.0.5
	docker pull mesosphere/marathon:v1.3.5
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

