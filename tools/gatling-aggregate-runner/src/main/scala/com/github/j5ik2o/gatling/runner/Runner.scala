package com.github.j5ik2o.gatling.runner

import java.text.SimpleDateFormat
import java.util.Date
import com.github.j5ik2o.reactive.aws.ecs.implicits._
import com.github.j5ik2o.reactive.aws.ecs.EcsAsyncClient
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.ecs.model._
import software.amazon.awssdk.services.ecs.{ EcsAsyncClient => JavaEcsAsyncClient }

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.compat.java8.OptionConverters._

object Runner extends App {

  def runTask(runTaskEcsClient: EcsAsyncClient,
              runTaskEcsClusterName: String,
              runTaskTaskDefinition: String,
              runTaskCount: Int,
              runTaskSubnets: Seq[String],
              runTaskAssignPublicIp: AssignPublicIp,
              runTaskContainerOverrideName: String,
              runTaskEnvironments: Map[String, String])(implicit ec: ExecutionContext): Future[Seq[Task]] = {
    val runTaskRequest = RunTaskRequest
      .builder()
      .cluster(runTaskEcsClusterName)
      .taskDefinition(runTaskTaskDefinition)
      .count(runTaskCount)
      .launchType(LaunchType.FARGATE)
      .networkConfiguration(
        NetworkConfiguration
          .builder().awsvpcConfiguration(
            AwsVpcConfiguration
              .builder()
              .subnets(runTaskSubnets.asJava)
              .assignPublicIp(runTaskAssignPublicIp)
              .build()
          ).build()
      )
      .overrides(
        TaskOverride
          .builder().containerOverrides(
            ContainerOverride
              .builder()
              .name(runTaskContainerOverrideName)
              .environment(
                runTaskEnvironments.map { case (k, v) => KeyValuePair.builder().name(k).value(v).build() }.toSeq.asJava
              ).build()
          ).build()
      )
      .build()
    val future = runTaskEcsClient.runTask(runTaskRequest).flatMap { result =>
      if (result.sdkHttpResponse().isSuccessful) {
        val tasks = result.tasks().asScala
        Future.successful(tasks)
      } else
        throw new Exception(result.failures().asScala.map(_.toString()).mkString(","))
    }
    future
  }

  val now            = new Date
  val logger         = LoggerFactory.getLogger(getClass)
  val config: Config = ConfigFactory.load()

  val underlying            = JavaEcsAsyncClient.builder().build()
  val client                = EcsAsyncClient(underlying)
  val runTaskEcsClusterName = config.as[String]("gatling.ecs-cluster-name")
  val runTaskTaskDefinition = config.as[String]("gatling.task-definition")
  val runTaskCount          = config.as[Int]("gatling.count")
  val runTaskSubnets        = config.as[Seq[String]]("gatling.subnets")
  val runTaskAssignPublicIp = AssignPublicIp.valueOf(config.as[String]("gatling.assign-public-ip"))

  val runTaskLogPrefix             = config.as[String]("gatling.log-prefix")
  val runTaskContainerOverrideName = config.as[String]("gatling.container-override-name")

  val df              = new SimpleDateFormat("YYYYMMDDHHmmss")
  val executionIdPath = runTaskLogPrefix + df.format(now) + "-" + now.getTime.toString

  logger.info(s"executionIdPath = $executionIdPath")

  val runTaskEnvironments = config.as[Map[String, String]]("gatling.environments") ++ Map(
    "TW_GATLING_EXECUTION_ID" -> executionIdPath
  )

  import scala.concurrent.ExecutionContext.Implicits.global

  val future = runTask(client,
                       runTaskEcsClusterName,
                       runTaskTaskDefinition,
                       runTaskCount,
                       runTaskSubnets,
                       runTaskAssignPublicIp,
                       runTaskContainerOverrideName,
                       runTaskEnvironments).flatMap { tasks =>
    val taskArns = tasks.map(_.taskArn())

    //scalastyle:off
    def loop(): Future[Unit] = {
      client
        .describeTasks(
          DescribeTasksRequest
            .builder().cluster(runTaskEcsClusterName).include(TaskField.knownValues()).tasksAsScala(taskArns).build()
        ).flatMap { res =>
          val list  = res.getValueForField("tasks", classOf[java.util.List[Task]])
          val tasks = list.asScala.flatMap(_.asScala.toList)
          if (tasks.exists(_.forall(_.lastStatus() == "STOPPED"))) {
            logger.info("Gatling completed")
            Thread.sleep(1000)
            val runTaskReporterTaskDefinition        = config.as[String]("gatling.reporter.task-definition")
            val runTaskReporterContainerOverrideName = config.as[String]("gatling.reporter.container-override-name")
            val runTaskReporterEnvironments = config.as[Map[String, String]]("gatling.reporter.environments") ++ Map(
              "TW_GATLING_RESULT_DIR_PATH" -> executionIdPath
            )
            runTask(
              client,
              runTaskEcsClusterName,
              runTaskReporterTaskDefinition,
              1,
              runTaskSubnets,
              runTaskAssignPublicIp,
              runTaskReporterContainerOverrideName,
              runTaskReporterEnvironments
            ).flatMap { reporterTasks =>
              val reporterTaskArns = reporterTasks.map(_.taskArn())
              def subLoop(): Future[Unit] = {
                client
                  .describeTasks(
                    DescribeTasksRequest
                      .builder().cluster(runTaskEcsClusterName).include(TaskField.knownValues()).tasksAsScala(
                        reporterTaskArns
                      ).build()
                  ).flatMap { res =>
                    val list          = res.getValueForField("tasks", classOf[java.util.List[Task]])
                    val reporterTasks = list.asScala.flatMap(_.asScala.toList)
                    if (reporterTasks.exists(_.forall(_.lastStatus() == "STOPPED"))) {
                      logger.info("Gatling Reporter completed")
                      logger.info(
                        s"report url: https://thread-weaver-gatling-logs.s3.amazonaws.com/$executionIdPath/index.html"
                      )
                      Future.successful(())
                    } else {
                      logger.info("---")
                      Thread.sleep(1000)
                      subLoop()
                    }
                  }
              }
              subLoop()
            }
          } else {
            logger.info("---")
            Thread.sleep(1000)
            loop()
          }
        }
    }
    loop()
  }
  Await.result(future, Duration.Inf)
}
