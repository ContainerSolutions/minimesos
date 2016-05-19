'use strict';

var os = require('os');
var exec = require('child_process').exec;

var Registrator = function () {

	var childProcesses = {}; //maps container ids to the dns-sd/avahi child proces

	var createCommand = function (container, hostName, ip) {
		if (os.platform() === 'darwin') {
			return "dns-sd -P " + container + " _http._tcp \"\" 1111 " + hostName + " " + ip;
		} else if (os.platform() === 'linux') {
			return "avahi-publish -a " + hostName + " " + ip;
		}
	};

	var killChild = function (container) {
		console.log("Unregistering process for container " + container);
		childProcesses[container].kill();
		delete childProcesses[container];
	};

	return {
		registerContainer: function (container, hostName, ip) {
			console.log("Publishing container " + container + " on ip " + ip + " with hostname: " + hostName);
			var command = createCommand(container, hostName, ip);
			childProcesses[container] = exec(command);
		},

		unregisterContainer: function (container) {
			if (container in childProcesses) {
				killChild(container);
			}
		},

		unregisterAllContainers: function () {
			for (var container in childProcesses) {
				killChild(container);
			}
		}
	}

};

module.exports = Registrator;