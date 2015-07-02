#!/usr/bin/env bash

for i in $(seq 1 $NUMBER_OF_SLAVES)
do
  mkdir -p /var/lib/mesos/$i
done

sed -i s/@@NUM_SLAVES@@/${NUMBER_OF_SLAVES}/g /etc/supervisor.conf
supervisord -c /etc/supervisor.conf

