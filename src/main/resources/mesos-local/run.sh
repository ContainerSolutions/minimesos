#!/usr/bin/env bash

IFACE='eth0'
IP=$(ip -4 address show $IFACE | grep 'inet' | sed 's/.*inet \([0-9\.]\+\).*/\1/')


echo "" >> /etc/hosts

for i in $(seq 1 $NUMBER_OF_SLAVES)
do
  mkdir -p /var/lib/mesos/$i
  echo "$IP slave$i" >> /etc/hosts
done

sed -i s/@@NUM_SLAVES@@/${NUMBER_OF_SLAVES}/g /etc/supervisor.conf
supervisord -c /etc/supervisor.conf

