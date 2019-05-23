package com.github.j5ik2o.threadWeaver.dynmodb

import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDbAsyncClient
import com.github.j5ik2o.reactive.aws.dynamodb.implicits._
import org.slf4j.Logger
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait DynamoDBCreator {
  def logger: Logger
  def dynamoDBEndpoint: String
  def awsAccessKeyId: String
  def awsSecretAccessKey: String

  def scalaClient: DynamoDbAsyncClient

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
    Await.result(scalaClient.createTable(createRequest), Duration.Inf)
    val result = Await.result(scalaClient.listTables(ListTablesRequest.builder().build()), Duration.Inf)
    logger.debug("createSnapshotTable: finish")
  }

  // scalastyle:off
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
    Await.result(
      scalaClient
        .createTable(createRequest),
      Duration.Inf
    )

    logger.debug("createJournalTable: finish")

  }
}
