package com.github.j5ik2o.threadWeaver.dynmodb
import java.net.URI

import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDbAsyncClient
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }

import scala.util.control.NonFatal

object Main extends App with DynamoDBCreator {
  val logger = LoggerFactory.getLogger(getClass)

  val awsAccessKeyId: String     = sys.env.getOrElse("DYNAMODB_AWS_ACCESS_KEY_ID", "x")
  val awsSecretAccessKey: String = sys.env.getOrElse("DYNAMODB_AWS_SECRET_ACCESS_KEY", "x")

  val dynamoDBHost: String = sys.env.getOrElse("DYNAMODB_HOST", "localhost")
  val dynamoDBPort: Int    = sys.env.getOrElse("DYNAMODB_PORT", "8000").toInt

  val dynamoDBEndpoint: String = s"http://$dynamoDBHost:$dynamoDBPort"

  lazy val javaClient: JavaDynamoDbAsyncClient = JavaDynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey))
    )
    .endpointOverride(URI.create(dynamoDBEndpoint))
    .build()

  lazy val scalaClient: DynamoDbAsyncClient = DynamoDbAsyncClient(javaClient)

  try {
    createJournalTable()
    createSnapshotTable()
  } catch {
    case NonFatal(ex) =>
      logger.error("occurred create table error", ex)
  }

}
