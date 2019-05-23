#!/bin/sh
cd `dirname $0`

AWS_ACCESS_KEY_ID=x AWS_SECRET_ACCESS_KEY=x aws dynamodb create-table \
    --endpoint-url http://localhost:8000 \
    --cli-input-json file://./table.json
