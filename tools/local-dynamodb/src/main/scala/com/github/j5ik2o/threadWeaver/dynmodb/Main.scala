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
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient => JavaDynamoDbAsyncClient }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal

@SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var"))
object Main extends App with DynamoDBCreator {
  val logger                       = LoggerFactory.getLogger(getClass)
  val sqlite4javaLibraryPath: File = new File("./native-libs")
  val region: Regions              = Regions.AP_NORTHEAST_1

  val awsAccessKeyId: String     = "x"
  val awsSecretAccessKey: String = "x"

  val dynamoDBHost: String = "127.0.0.1"
  val dynamoDBPort: Int    = 8000

  val dynamoDBEndpoint: String = s"http://$dynamoDBHost:$dynamoDBPort"

  val waitIntervalForDynamoDBLocal = 1 seconds

  lazy val javaClient: JavaDynamoDbAsyncClient = JavaDynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey))
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
