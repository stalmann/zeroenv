#!/usr/bin/env bash
docker-compose up -d
sleep 5
./auth/install_realm.sh
