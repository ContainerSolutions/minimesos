#!/usr/local/bin/node

var Dockerode = require('dockerode');
var DockerEvents = require('docker-events');
var dockerode = new Dockerode();
var Registrator = require('./registrator.js');
var registrator = new Registrator();

var emitter = new DockerEvents({
	docker: dockerode
});

console.log("Started listening for Docker events");

emitter.start();

// TODO Also check cluster ID

emitter.on("start", function (message) {
	if (isMinimesosContainerEvent(message)) {
		var name = message.Actor.Attributes.name;
		var container = dockerode.getContainer(message.id);

		container.inspect(function (err, data) {
			var ipAddress = data.NetworkSettings.IPAddress;
			if (ipAddress) {
				var parts = name.split("-");
				var role = parts[1];
				var hostName = role + ".mycluster.local";
				registrator.registerContainer(name, hostName, ipAddress);
			}
		});
	}
});

emitter.on("die", function (message) {
	if (isMinimesosContainerEvent(message)) {
		registrator.unregisterContainer(message.Actor.Attributes.name);
	}
});

function isMinimesosContainerEvent(message) {
	return message.Actor != null
			&& message.Actor.Attributes != null
			&& message.Actor.Attributes.name.startsWith("minimesos");
}
