#!/bin/bash

# mount S3 bucket
if [ ! -d $S3_MOUNT_POINT ]
then
  mkdir $S3_MOUNT_POINT
fi

mountpoint $S3_MOUNT_POINT
ret=$?
if [ $ret -eq 1 ]
then
  $GOPATH/bin/goofys $BUCKET_NAME $S3_MOUNT_POINT
  echo "$BUCKET_NAME is mounted.($S3_MOUNT_POINT)"
fi

# set gatling_execution_id
gatling_execution_id=${1:-`ls $S3_MOUNT_POINT | grep "^[0-9]*$" | sort -nr | head -n 1`}
if [ -z "$gatling_execution_id" ]
then
  echo "error: gatling_execution_id is empty." > /dev/stderr
  exit 1
fi
echo gatling_execution_id is $gatling_execution_id

# execute gatling.sh
~/gatling/bin/gatling.sh -ro $S3_MOUNT_POINT/$gatling_execution_id/

# fix content-type
function fix_content_type () {
  aws s3 ls --recursive s3://$BUCKET_NAME/$gatling_execution_id/ | grep -e "$1$" | awk '{ print "s3://'"${BUCKET_NAME}"'/"$4 }'   | while read origname; do aws s3 mv ${origname} ${origname}.bak; aws s3 mv ${origname}.bak ${origname} --metadata-directive REPLACE --content-type $2; done
}

fix_content_type .jpg image/jpg
fix_content_type .gif image/gif
fix_content_type .png image/png
fix_content_type .ico image/x-icon
fix_content_type .html text/html
fix_content_type .css text/css
fix_content_type .js text/javascript
fix_content_type .xml text/xml
fix_content_type .json application/json

echo [report url] http://$BUCKET_NAME.s3.amazonaws.com/$gatling_execution_id/index.html
