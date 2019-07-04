FROM openjdk:8-jre-alpine

RUN apk --no-cache add --update bash openssl curl

# Add the flyway user and step in the directory
RUN adduser -S -h /flyway -D flyway
WORKDIR /flyway

# Change to the flyway user
USER flyway

ENV FLYWAY_VERSION 5.2.0
ENV MYSQL_DRIVER_VERSION 5.1.47

RUN curl -L https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/${FLYWAY_VERSION}/flyway-commandline-${FLYWAY_VERSION}.tar.gz -o flyway-commandline-${FLYWAY_VERSION}.tar.gz \
  && tar -xzf flyway-commandline-${FLYWAY_VERSION}.tar.gz --strip-components=1 \
  && rm flyway-commandline-${FLYWAY_VERSION}.tar.gz

RUN rm -f /flyway/drivers/mysql-*.jar

ADD https://repo1.maven.org/maven2/mysql/mysql-connector-java/${MYSQL_DRIVER_VERSION}/mysql-connector-java-${MYSQL_DRIVER_VERSION}.jar /flyway/jars/

COPY ./src/test/resources/db-migration/* /flyway/sql

ENTRYPOINT ["/flyway/flyway"]
CMD ["-?"]