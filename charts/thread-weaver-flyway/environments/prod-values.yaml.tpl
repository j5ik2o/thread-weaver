image:
  repository: ${ACCOUNT_ID}.dkr.ecr.ap-northeast-1.amazonaws.com/j5ik2o/thread-weaver-flyway
  tag: latest
  pullPolicy: Always
secrets:
  flyway: |
    flyway.driver=com.mysql.jdbc.Driver
    flyway.url=jdbc:mysql://${FLYWAY_HOST}:${FLYWAY_PORT}/${FLYWAY_DB}?useSSL=false
    flyway.user=${FLYWAY_USER}
    flyway.password=${FLYWAY_PASSWORD}
