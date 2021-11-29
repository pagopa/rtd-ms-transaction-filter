#!/bin/sh

base64 -d /tmp/certs.jks > /app/certs.jks &&
	sleep 600
