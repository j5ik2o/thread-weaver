import com.amazonaws.regions.{ Region, Regions }
import com.github.j5ik2o.reactive.aws.ecs._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.internal.util.ManagedLogger
import sbtecr.EcrPlugin.autoImport._
import software.amazon.awssdk.services.ecs.model.{ Task, _ }
import software.amazon.awssdk.services.ecs.{ EcsAsyncClient => JavaEcsAsyncClient }
import wartremover.WartRemover.autoImport._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

object Settings {
  val akkaVersion           = "2.5.22"
  val akkaHttpVersion       = "10.1.8"
  val akkaManagementVersion = "1.0.0"
  val circeVersion          = "0.11.1"
  val monocleVersion        = "1.5.0"
  val swaggerVersion        = "2.0.8"
  val slickVersion          = "3.2.3"
  val gatlingVersion        = "3.1.2"
  val awsSdkVersion         = "1.11.575"

  val compileScalaStyle = taskKey[Unit]("compileScalaStyle")

  val ecrSettings = Seq(
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := "j5ik2o/thread-weaver-api-server",
    repositoryTags in Ecr ++= Seq(version.value),
    localDockerImage in Ecr := "j5ik2o/" + (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
  )

  lazy val dockerCommonSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u191-b12",
    maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
    dockerUpdateLatest := true,
    bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
      "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
      "addJava -Dakka.remote.startup-timeout=60s"
    )
  )

  val baseSettings = Seq(
    scalaVersion := "2.12.8",
    version := "1.0.0-SNAPSHOT",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-language:_",
      // Warn if an argument list is modified to match the receiver
      "-Ywarn-adapted-args",
      // Warn when dead code is identified.
      "-Ywarn-dead-code",
      // Warn about inaccessible types in method signatures.
      "-Ywarn-inaccessible",
      // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-infer-any",
      // Warn when non-nullary `def f()' overrides nullary `def f'
      "-Ywarn-nullary-override",
      // Warn when nullary methods return Unit.
      "-Ywarn-nullary-unit",
      // Warn when numerics are widened.
      "-Ywarn-numeric-widen",
      // Warn when imports are unused.
      "-Ywarn-unused-import"
    ),
    scalafmtOnCompile in ThisBuild := true,
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full),
    wartremoverErrors in (Compile, compile) ++= Seq(
      Wart.ArrayEquals,
      Wart.AnyVal,
      Wart.Var,
      Wart.Null,
      Wart.OptionPartial
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      "DynamoDB Local Repository" at "https://s3-ap-northeast-1.amazonaws.com/dynamodb-local-tokyo/release",
      Resolver.bintrayRepo("segence", "maven-oss-releases"),
      Resolver.bintrayRepo("everpeace", "maven"),
      Resolver.bintrayRepo("tanukkii007", "maven"),
      Resolver.bintrayRepo("kamon-io", "snapshots")
    ),
    libraryDependencies ++= Seq(
      "io.monix"           %% "monix"          % "3.0.0-RC2",
      "eu.timepit"         %% "refined"        % "0.9.5",
      "org.wvlet.airframe" %% "airframe"       % "19.4.1",
      "org.scalatest"      %% "scalatest"      % "3.0.4" % Test,
      "ch.qos.logback"     % "logback-classic" % "1.2.3" % Test
    ),
    dependencyOverrides ++= Seq(
      "org.slf4j"                  % "slf4j-api"               % "1.7.26",
      "org.scala-lang.modules"     %% "scala-xml"              % "1.2.0",
      "com.typesafe.akka"          %% "akka-slf4j"             % "2.5.22",
      "com.typesafe.akka"          %% "akka-actor"             % "2.5.22",
      "com.typesafe.akka"          %% "akka-stream"            % "2.5.22",
      "com.typesafe.akka"          %% "akka-cluster"           % "2.5.22",
      "com.typesafe.akka"          %% "akka-cluster-sharding"  % "2.5.22",
      "com.typesafe.akka"          %% "akka-discovery"         % "2.5.22",
      "com.typesafe.akka"          %% "akka-persistence"       % "2.5.22",
      "com.typesafe.akka"          %% "akka-persistence-query" % "2.5.22",
      "com.typesafe.akka"          %% "akka-testkit"           % "2.5.22",
      "com.typesafe.akka"          %% "akka-http"              % "10.1.8",
      "com.typesafe.akka"          %% "akka-http-core"         % "10.1.8",
      "com.typesafe.akka"          %% "akka-parsing"           % "10.1.8",
      "org.typelevel"              %% "cats-core"              % "1.5.0",
      "org.typelevel"              %% "cats-kernel"            % "1.5.0",
      "org.typelevel"              %% "cats-macros"            % "1.5.0",
      "org.typelevel"              %% "machinist"              % "0.6.6",
      "com.typesafe"               % "config"                  % "1.3.1",
      "com.typesafe"               %% "ssl-config-core"        % "0.3.6",
      "io.netty"                   % "netty-codec-http"        % "4.1.33.Final",
      "io.netty"                   % "netty-handler"           % "4.1.33.Final",
      "org.scala-lang.modules"     %% "scala-java8-compat"     % "0.9.0",
      "com.google.guava"           % "guava"                   % "27.1-jre",
      "com.lihaoyi"                %% "sourcecode"             % "0.1.4",
      "org.reactivestreams"        % "reactive-streams"        % "1.0.2",
      "com.google.errorprone"      % "error_prone_annotations" % "2.3.2",
      "de.heikoseeberger"          %% "akka-http-circe"        % "1.25.2",
      "io.circe"                   %% "circe-parser"           % "0.11.1",
      "io.circe"                   %% "circe-generic"          % "0.11.1",
      "io.circe"                   %% "circe-core"             % "0.11.1",
      "io.swagger.core.v3"         % "swagger-jaxrs2"          % "2.0.8",
      "io.swagger.core.v3"         % "swagger-core"            % "2.0.8",
      "io.swagger.core.v3"         % "swagger-models"          % "2.0.8",
      "io.swagger.core.v3"         % "swagger-annotations"     % "2.0.8",
      "com.fasterxml.jackson.core" % "jackson-annotations"     % "2.9.8"
    ),
    parallelExecution in Test := false,
    fork := true,
    (scalastyleConfig in Compile) := file("scalastyle-config.xml"),
    compileScalaStyle := scalastyle.in(Compile).toTask("").value,
    (compile in Compile) := (compile in Compile).dependsOn(compileScalaStyle).value
  )

  lazy val gatlingAggregateRunnerEcrSettings = Seq(
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := "j5ik2o/thread-weaver-gatling-aggregate-runner",
    localDockerImage in Ecr := "j5ik2o/" + (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
  )

  lazy val gatlingRunnerEcrSettings = Seq(
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := "j5ik2o/thread-weaver-gatling-runner",
    localDockerImage in Ecr := "j5ik2o/" + (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value
  )

  lazy val gatlingCommonSettings = Seq(
    organization := "com.github.j5ik2o",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-language:_",
      // Warn if an argument list is modified to match the receiver
      "-Ywarn-adapted-args",
      // Warn when dead code is identified.
      "-Ywarn-dead-code",
      // Warn about inaccessible types in method signatures.
      "-Ywarn-inaccessible",
      // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-infer-any",
      // Warn when non-nullary `def f()' overrides nullary `def f'
      "-Ywarn-nullary-override",
      // Warn when nullary methods return Unit.
      "-Ywarn-nullary-unit",
      // Warn when numerics are widened.
      "-Ywarn-numeric-widen",
      // Warn when imports are unused.
      "-Ywarn-unused-import",
      "-Ywarn-numeric-widen"
    )
  )

  val gatling = taskKey[Unit]("gatling")
  val runTask = taskKey[Seq[Task]]("run-task")

  val runTaskEcsClient             = settingKey[EcsAsyncClient]("run-task-ecs-client")
  val runTaskAwaitDuration         = settingKey[Duration]("run-task-await-duration")
  val runTaskEcsCluster            = settingKey[String]("run-task-ecs-cluster")
  val runTaskTaskDefinition        = taskKey[String]("run-task-task-definition")
  val runTaskSubnets               = settingKey[Seq[String]]("run-task-subnets")
  val runTaskAssignPublicIp        = settingKey[AssignPublicIp]("run-task-assign-public-ip")
  val runTaskEnvironments          = taskKey[Map[String, String]]("run-task-environments")
  val runTaskContainerOverrideName = settingKey[String]("run-task-container-override-name")

  val gatlingAggregateRunTaskSettings = Seq(
    runTaskEcsClient in gatling := {
      val underlying = JavaEcsAsyncClient
        .builder()
        .build()
      EcsAsyncClient(underlying)
    },
    runTaskEcsCluster in gatling := "j5ik2o-gatling-ecs",
    runTaskTaskDefinition in gatling := "j5ik2o-gatling-aggregate-runner:2",
    runTaskAwaitDuration in gatling := Duration.Inf,
    runTaskSubnets in gatling := Seq("subnet-096d7af9e31f4f8c7"), // 10.0.1.0/24 public
    runTaskAssignPublicIp in gatling := AssignPublicIp.ENABLED,
    runTaskEnvironments in gatling := Map(
      "AWS_REGION"                          -> "ap-northeast-1",
      "TW_GATLING_ECS_CLUSTER_NAME"         -> (runTaskEcsCluster in gatling).value,
      "TW_GATLING_SUBNET"                   -> (runTaskSubnets in gatling).value.head,
      "TW_GATLING_TASK_DEFINITION"          -> "j5ik2o-gatling-runner:4",
      "TW_GATLING_COUNT"                    -> "1",
      "TW_GATLING_HOLD_DURATION"            -> "1m",
      "TW_GATLING_RAMP_DURATION"            -> "3m",
      "TW_GATLING_ENDPOINT_BASE_URL"        -> "http://af1003d2a9f3b11e9a8db0a582f897d4-1563252270.ap-northeast-1.elb.amazonaws.com:8080/v1",
      "TW_GATLING_SIMULATION_CLASS"         -> "com.github.j5ik2o.gatling.ThreadSimulation",
      "TW_GATLING_USERS"                    -> "3",
      "TW_GATLING_REPORTER_TASK_DEFINITION" -> "j5ik2o-gatling-s3-reporter:6",
      "TW_GATLING_BUCKET_NAME"              -> "thread-weaver-gatling-logs"
    ),
    runTaskContainerOverrideName in gatling := "gatling-aggregate-runner",
    runTask in gatling := {
      implicit val log                  = streams.value.log
      val _runTaskEcsClient             = (runTaskEcsClient in gatling).value
      val _runTaskEcsCluster            = (runTaskEcsCluster in gatling).value
      val _runTaskTaskDefinition        = (runTaskTaskDefinition in gatling).value
      val _runTaskSubnets               = (runTaskSubnets in gatling).value
      val _runTaskAssignPublicIp        = (runTaskAssignPublicIp in gatling).value
      val _runTaskContainerOverrideName = (runTaskContainerOverrideName in gatling).value
      val _runTaskEnvironments          = (runTaskEnvironments in gatling).value
      log.info("start runGatlingTask")
      val future = runGatlingTask(
        _runTaskEcsClient,
        _runTaskEcsCluster,
        _runTaskTaskDefinition,
        1,
        _runTaskSubnets,
        _runTaskAssignPublicIp,
        _runTaskContainerOverrideName,
        _runTaskEnvironments
      )
      val result = Await.result(future, (runTaskAwaitDuration in gatling).value)
      result.foreach { task: Task =>
        log.info(s"task.arn = ${task.taskArn()}")
      }
      log.info("finish runGatlingTask")
      result
    }
  )

  def runGatlingTask(
      runTaskEcsClient: EcsAsyncClient,
      runTaskEcsCluster: String,
      runTaskTaskDefinition: String,
      runTaskCount: Int,
      runTaskSubnets: Seq[String],
      runTaskAssignPublicIp: AssignPublicIp,
      runTaskContainerOverrideName: String,
      runTaskEnvironments: Map[String, String]
  )(implicit log: ManagedLogger): Future[Seq[Task]] = {
    val runTaskRequest = RunTaskRequest
      .builder()
      .cluster(runTaskEcsCluster)
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

}
