#!/usr/bin/env bash
docker network rm account-bridge-nw
docker container rm elastic -f
docker network create -d bridge account-bridge-nw
export ELASTIC_HOSTS=http://elastic:9200
docker run -itd -p 9200:9200 --network=account-bridge-nw --name=elastic  vnair5/elastic-search
docker run -itd -p 50051:50051 --env ELASTIC_HOSTS --network=account-bridge-nw vnair5/account-search-service:latest