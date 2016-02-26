package clusterconfig

minimesos {
    agent {
        resources {
            cpu {
                role  = "*"
                value = 2
            }
            mem {
                role  = "*"
                value = 1024
            }
            disk {
                role = "*"
                value = 8192
            }
        }
    }
    agent {
    }
}