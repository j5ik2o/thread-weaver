#!/bin/sh

sbt -Dmysql.host=localhost -Dmysql.port=30306 'migrate-mysql/run'
DYNAMODB_HOST=localhost DYNAMODB_PORT=32000 sbt 'migrate-dynamodb/run'
