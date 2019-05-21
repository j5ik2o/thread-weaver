package com.github.j5ik2o.threadWeaver.dynmodb

import java.io.File
import java.net.URI

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.local.server.{
  DynamoDBProxyServer,
  LocalDynamoDBRequestHandler,
  LocalDynamoDBServerHandler
}
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDbAsyncClient
import com.github.j5ik2o.reactive.aws.dynamodb.implicits._
import org.slf4j.{ Logger, LoggerFactory }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }
import software.amazon.awssdk.services.dynamodb.model._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

@SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var"))
object Main extends App with DynamoDBCreator {
  val logger                       = LoggerFactory.getLogger(getClass)
  val sqlite4javaLibraryPath: File = new File("./native-libs")
  val region: Regions              = Regions.AP_NORTHEAST_1
  val accessKeyId: String          = "x"
  val secretAccessKey: String      = "x"
  val dynamoDBPort: Int            = 8000
  val dynamoDBEndpoint: String     = s"http://127.0.0.1:$dynamoDBPort"
  val waitIntervalForDynamoDBLocal = 1 seconds

  lazy val javaClient: JavaDynamoDbAsyncClient = JavaDynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
    )
    .endpointOverride(URI.create(dynamoDBEndpoint))
    .build()

  lazy val scalaClient: DynamoDbAsyncClient = DynamoDbAsyncClient(javaClient)

  val dynamoDBProxyServer: DynamoDBProxyServer = {
    System.setProperty("sqlite4java.library.path", sqlite4javaLibraryPath.toString)
    val inMemory = true
    // scalastyle:off
    val dbPath     = null
    val sharedDb   = false
    val corsParams = null
    // scalastyle:on
    new DynamoDBProxyServer(
      dynamoDBPort,
      new LocalDynamoDBServerHandler(
        new LocalDynamoDBRequestHandler(0, inMemory, dbPath, sharedDb, false),
        corsParams
      )
    )
  }

  def waitDynamoDBLocal(): Unit = {
    var isWaken: Boolean = false
    while (!isWaken) {
      try {
        Await.result(scalaClient.listTables(), Duration.Inf)
        isWaken = true
      } catch {
        case _: Exception =>
          logger.info("waiting...")
          Thread.sleep(waitIntervalForDynamoDBLocal.toMillis)
      }
    }
  }

  try {
    dynamoDBProxyServer.start()
    waitDynamoDBLocal()
    logger.info("dynmodb local started")
  } catch {
    case NonFatal(ex) =>
      logger.error("occurred dynamodb start error", ex)
  }

  try {
    createJournalTable()
    createSnapshotTable()
  } catch {
    case NonFatal(ex) =>
      logger.error("occurred create table error", ex)
  }

  sys.addShutdownHook {
    dynamoDBProxyServer.stop()
    javaClient.close()
    logger.info("dynamodb local stopped")
  }

}

trait DynamoDBCreator {
  def logger: Logger
  def dynamoDBEndpoint: String
  def accessKeyId: String
  def secretAccessKey: String

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
