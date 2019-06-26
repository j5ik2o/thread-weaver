package com.github.j5ik2o.gatling.runner

import java.text.SimpleDateFormat
import java.util.Date

import com.github.j5ik2o.reactive.aws.ecs.EcsAsyncClient
import software.amazon.awssdk.services.ecs.model._

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import software.amazon.awssdk.services.ecs.{ EcsAsyncClient => JavaEcsAsyncClient }

import net.ceedubs.ficus.Ficus._
import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.LoggerFactory

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

  val underlying            = JavaEcsAsyncClient.create()
  val client                = EcsAsyncClient(underlying)
  val runTaskEcsClusterName = config.as[String]("ecs-cluster-name")
  val runTaskTaskDefinition = config.as[String]("task-definition")
  val runTaskCount          = config.as[Int]("count")
  val runTaskSubnets        = config.as[Seq[String]]("subnets")
  val runTaskAssignPublicIp = AssignPublicIp.valueOf(config.as[String]("assign-public-ip"))

  val runTaskLogPrefix             = config.as[String]("log-prefix")
  val runTaskContainerOverrideName = config.as[String]("container-override-name")

  val df = new SimpleDateFormat("YYYYMMDDHHmmss")

  val runTaskEnvironments = config.as[Map[String, String]]("environments") ++ Map(
    "GAUDI_POC_GATLING_EXECUTION_ID" -> (runTaskLogPrefix + df.format(now) + "-" + now.getTime.toString)
  )

  import scala.concurrent.ExecutionContext.Implicits.global
  runTask(client,
          runTaskEcsClusterName,
          runTaskTaskDefinition,
          runTaskCount,
          runTaskSubnets,
          runTaskAssignPublicIp,
          runTaskContainerOverrideName,
          runTaskEnvironments).flatMap { tasks =>
    val taskArn = tasks.head.taskArn()

    def loop(): Future[Unit] = {
      client.describeTasks(DescribeTasksRequest.builder().tasks(taskArn).build()).flatMap { res =>
        val list = res.getValueForField("tasks", classOf[java.util.List[Task]])
        val task = list.get().asScala.head
        logger.info(s"task = $task")
        if (task.lastStatus() == "STOPPED")
          Future.successful(())
        else {
          logger.info("---")
          Thread.sleep(1000)
          loop()
        }
      }
    }
    loop()
  }
}
