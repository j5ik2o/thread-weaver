# Default values for astraea-elasticmq.
# This is a YAML-formatted file.
# Declare variables to be passed into your templates.
envName: production
prodEnabled: true
configResource: production.conf
jvmHeapMin: 1500m
jvmHeapMax: 1500m
jvmMetaMax: 500m
replicaCount: 3
image:
  repository: ${ACCOUNT_ID}.dkr.ecr.ap-northeast-1.amazonaws.com/j5ik2o/thread-weaver-api-server
  tag: latest
  pullPolicy: Always
service:
  type: LoadBalancer
  api:
    name: api
    externalPort: 18080
    internalPort: 18080
  management:
    name: management
    externalPort: 18558
    internalPort: 18558
resources:
  requests:
    cpu: 2
    memory: 4Gi
  limits:
    cpu: 2
    memory: 4Gi
db:
  url: jdbc:mysql://j5ik2o-rds-cluster-aurora.cluster-ctywrcabnmgr.ap-northeast-1.rds.amazonaws.com:3306/thread_weaver?useSSL=false
  user: root
  maxPoolSize: 10
  minIdleSize: 10
dynamodb:
  journalTableName: thread_weaver_journal
  snapshotTableName: thread_weaver_snapshot
  readJournalTableName : thread_weaver_journal