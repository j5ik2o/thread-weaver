package com.github.j5ik2o.threadWeaver.adaptor.util

import java.net.URI

import com.github.j5ik2o.reactive.aws.dynamodb.implicits._
import com.github.j5ik2o.reactive.aws.dynamodb.{ DynamoDBEmbeddedSpecSupport, DynamoDbAsyncClient }
import org.scalatest.{ Matchers, Suite }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.slf4j.{ Logger, LoggerFactory }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.duration._
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }

trait DynamoDBSpecSupport extends DynamoDBEmbeddedSpecSupport with ScalaFutures with Matchers with Eventually {
  this: Suite =>

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private implicit val pc: PatienceConfig = PatienceConfig(20 seconds, 1 seconds)

  lazy val javaClient: JavaDynamoDbAsyncClient = JavaDynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
    )
    .endpointOverride(URI.create(dynamoDBEndpoint))
    .build()

  lazy val scalaClient: DynamoDbAsyncClient = DynamoDbAsyncClient(javaClient)

  def createSnapshotTable(): Unit = {
    logger.debug("createSnapshotTable: start")
    val tableName = "Snapshot"
    val createRequest = CreateTableRequest
      .builder()
      .attributeDefinitionsAsScala(
        Seq(
          AttributeDefinition
            .builder()
            .attributeName("persistence-id")
            .attributeType(ScalarAttributeType.S).build(),
          AttributeDefinition
            .builder()
            .attributeName("sequence-nr")
            .attributeType(ScalarAttributeType.N).build()
        )
      )
      .keySchemaAsScala(
        Seq(
          KeySchemaElement
            .builder()
            .attributeName("persistence-id")
            .keyType(KeyType.HASH).build(),
          KeySchemaElement
            .builder()
            .attributeName("sequence-nr")
            .keyType(KeyType.RANGE).build()
        )
      )
      .provisionedThroughput(
        ProvisionedThroughput
          .builder()
          .readCapacityUnits(10L)
          .writeCapacityUnits(10L).build()
      )
      .tableName(tableName).build()
    val createResponse = scalaClient
      .createTable(createRequest).futureValue
    eventually {
      val result = scalaClient.listTables(ListTablesRequest.builder().build()).futureValue
      result.tableNames.fold(false)(_.contains(tableName)) shouldBe true
    }
    logger.debug("createSnapshotTable: finish")
    createResponse.sdkHttpResponse().isSuccessful shouldBe true
  }

  def createJournalTable(): Unit = {
    logger.debug("createJournalTable: start")
    val tableName = "Journal"
    val createRequest = CreateTableRequest
      .builder()
      .tableName(tableName)
      .attributeDefinitionsAsScala(
        Seq(
          AttributeDefinition.builder
            .attributeName("pkey")
            .attributeType(ScalarAttributeType.S).build(),
          AttributeDefinition.builder
            .attributeName("persistence-id")
            .attributeType(ScalarAttributeType.S).build(),
          AttributeDefinition.builder
            .attributeName("sequence-nr")
            .attributeType(ScalarAttributeType.N).build(),
          AttributeDefinition.builder
            .attributeName("tags")
            .attributeType(ScalarAttributeType.S).build()
        )
      ).keySchemaAsScala(
        Seq(
          KeySchemaElement
            .builder()
            .attributeName("pkey")
            .keyType(KeyType.HASH).build(),
          KeySchemaElement
            .builder()
            .attributeName("sequence-nr")
            .keyType(KeyType.RANGE).build()
        )
      ).provisionedThroughput(
        ProvisionedThroughput
          .builder()
          .readCapacityUnits(10L)
          .writeCapacityUnits(10L).build()
      ).globalSecondaryIndexesAsScala(
        Seq(
          GlobalSecondaryIndex
            .builder()
            .indexName("TagsIndex")
            .keySchemaAsScala(
              Seq(
                KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("tags").build()
              )
            ).projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .provisionedThroughput(
              ProvisionedThroughput
                .builder()
                .readCapacityUnits(10L)
                .writeCapacityUnits(10L).build()
            ).build(),
          GlobalSecondaryIndex
            .builder()
            .indexName("GetJournalRowsIndex").keySchemaAsScala(
              Seq(
                KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("persistence-id").build(),
                KeySchemaElement.builder().keyType(KeyType.RANGE).attributeName("sequence-nr").build()
              )
            ).projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .provisionedThroughput(
              ProvisionedThroughput
                .builder()
                .readCapacityUnits(10L)
                .writeCapacityUnits(10L).build()
            ).build()
        )
      ).build()
    val createResponse = scalaClient
      .createTable(createRequest).futureValue

    eventually {
      val result = scalaClient.listTables(ListTablesRequest.builder().build()).futureValue
      result.tableNames.fold(false)(_.contains(tableName)) shouldBe true
    }

    logger.debug("createJournalTable: finish")
    createResponse.sdkHttpResponse().isSuccessful shouldBe true

  }
}
