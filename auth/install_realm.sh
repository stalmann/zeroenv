#!/usr/bin/env bash
until [ "`docker inspect -f {{.State.Health.Status}} auth`"=="healthy" ]; do
    echo waiting for container to be healthy
    sleep 1;
done;

echo installing test realm and test user

docker exec -it auth bash -c '\
  ./keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user "$(cat /run/secrets/keycloak_user_secret)"  --password="$(cat /run/secrets/keycloak_user_secret)";\
  ./keycloak/bin/kcadm.sh create realms -s realm=test -s enabled=true;\
  CID=$(./keycloak/bin/kcadm.sh create clients -r test -s clientId=testclient -s "redirectUris=[\"http://localhost:8081/*\"]" -i);\
  ./keycloak/bin/kcadm.sh get clients/$CID/installation/providers/keycloak-oidc-keycloak-json'