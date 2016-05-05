#!/usr/bin/env bash

# This script periodically wakes up and look for pants ConsoleRunner process,
# runs jstack against it if it finds one, to run on travis for debug purpose.

NUM_SAMPLES=30
SLEEP_INTERNAL_SECONDS=60


i="1"

while [ ${i} -le ${NUM_SAMPLES} ]
do
	PID=`ps -ef|grep ConsoleRunner|grep -v grep|awk '{print $2}'`
	if [ ! -z ${PID} ]; then
		echo "============================================================================="
		echo "Sampling jstack for pants ConsoleRunner process: ${PID} (${i}/${NUM_SAMPLES})"
		echo "============================================================================="
		echo
		jstack ${PID}
	fi

	sleep ${SLEEP_INTERNAL_SECONDS}
	i=$[${i}+1]
done

