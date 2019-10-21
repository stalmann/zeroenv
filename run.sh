#!/usr/bin/env bash
docker-compose up -d
./auth/install_realm.sh
