#!/bin/sh

cd $(dirname $0)
docker run --rm -v $(pwd)/../../../tools/flyway/src/test/resources/db-migration:/flyway/sql -v $(pwd):/flyway/conf boxfuse/flyway migrate

