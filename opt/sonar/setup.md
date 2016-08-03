### Create Persistent Disk
`gcloud compute disks create --size 200GB minimesos-sonar-postgres-disk`

### Attach created disk to linux instance for formatting and data transfer
`gcloud compute instances attach-disk jenkins-ci-4 --disk minimesos-sonar-postgres-disk --device-name postgresdisk`

### Mount and format disk
`/usr/share/google/safe_format_and_mount  /dev/disk/by-id/google-postgresdisk /postgresdisk`

### Detach Disk from linux instance
`gcloud compute instances detach-disk jenkins-ci-4 --disk minimesos-sonar-postgres-disk`

### Create cluster
`gcloud container clusters create "minimesos-sonar" --zone "europe-west1-d" --machine-type "n1-standard-2" --num-nodes "1" --network "ci-network" --enable-cloud-logging`

Download cluster credentials into kubectl:
`gcloud container clusters get-credentials minimesos-sonar`

### Create database password secret
This password gets applied to the postgres database on first start, changin it later is not possible as it's persisted
to the persistent disk

echo -n "thepassword" > password
`kubectl create secret generic postgres-pwd --from-file=./password`

### Create Certificate

kubectl create -f certificate.yaml



1. Create new domain name as old one can't be shared anymore between Jenkins and Sonar. sonar.minimesos.ci.container-solutions.com
2. Make https work for sonar
3. User management in sonar?
