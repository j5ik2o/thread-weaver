import Settings._
import com.typesafe.sbt.packager.docker._

val `infrastructure` = (project in file("infrastructure"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-infrastructure",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "de.huxhorn.sulky" % "de.huxhorn.sulky.ulid" % "8.2.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
      "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion
    )
  )

val `domain` = (project in file("domain"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-domain"
  ).dependsOn(`infrastructure`)


val `contract-use-case` = (project in file("contract-use-case"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-contract-use-case",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    )
  )
  .dependsOn(`domain`)

val `contract-interface` = (project in file("contract-interface"))
  .settings(baseSettings)
  .settings(
    name := "reaction-contract-interface",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
      "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.0.2",
      "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.0.3",
      "io.swagger.core.v3" % "swagger-core" % swaggerVersion,
      "io.swagger.core.v3" % "swagger-annotations" % swaggerVersion,
      "io.swagger.core.v3" % "swagger-models" % swaggerVersion,
      "io.swagger.core.v3" % "swagger-jaxrs2" % swaggerVersion
    )
  )
  .dependsOn(`contract-use-case`)

val `use-case` = (project in file("use-case"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-use-case",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
    )
  ).dependsOn(`contract-use-case`, `contract-interface`, `domain`, `infrastructure`)

val interface = (project in file("interface"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-interface",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.25.2",
      "ch.megard" %% "akka-http-cors" % "0.4.0",
      "com.github.j5ik2o" %% "akka-persistence-dynamodb" % "1.0.2",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" % Test,
      "org.iq80.leveldb" % "leveldb" % "0.9" % Test,
      "commons-io" % "commons-io" % "2.4" % Test,
      "com.github.j5ik2o" %% "reactive-aws-dynamodb-core" % "1.1.0" % Test,
      "com.github.j5ik2o" %% "reactive-aws-dynamodb-test" % "1.1.0" % Test
    ),
    // --- sbt-multi-jvm用の設定
    compile in MultiJvm := (compile in MultiJvm).triggeredBy(compile in Test).value,
    executeTests in Test := Def.task {
      val testResults = (executeTests in Test).value
      val multiNodeResults = (executeTests in MultiJvm).value
      val overall = (testResults.overall, multiNodeResults.overall) match {
        case (TestResult.Passed, TestResult.Passed) => TestResult.Passed
        case (TestResult.Error, _) => TestResult.Error
        case (_, TestResult.Error) => TestResult.Error
        case (TestResult.Failed, _) => TestResult.Failed
        case (_, TestResult.Failed) => TestResult.Failed
      }
      Tests.Output(overall,
        testResults.events ++ multiNodeResults.events,
        testResults.summaries ++ multiNodeResults.summaries)
    }.value,
    assemblyMergeStrategy in(MultiJvm, assembly) := {
      case "application.conf" => MergeStrategy.concat
      case "META-INF/aop.xml" => MergeStrategy.concat
      case x =>
        val old = (assemblyMergeStrategy in(MultiJvm, assembly)).value
        old(x)
    },
    Test / fork := true
    , logLevel := Level.Debug
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .dependsOn(`contract-interface`, `use-case`, `infrastructure`)

val api = (project in file("api"))
  .enablePlugins(AshScriptPlugin, JavaAgent)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(
    name := "thread-weaver-api",
    dockerBaseImage := "openjdk:8",
    dockerUsername := Some("j5ik2o"),
    fork in run := true,
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
      s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Dcom.sun.management.jmxremote.local.only=false",
      "-Dcom.sun.management.jmxremote"
    ),
    javaOptions in Universal ++= Seq(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.local.only=true",
      "-Dcom.sun.management.jmxremote.authenticate=false"
    ),
    libraryDependencies ++= Seq(
      "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
      "com.github.TanUkkii007" %% "akka-cluster-custom-downing" % "0.0.12",
      "org.slf4j" % "jul-to-slf4j" % "1.7.26",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  ).dependsOn(`interface`)


val gatlingVersion                 = "2.2.3"
val awsSdkVersion = "1.11.169"

lazy val `gatling-test` = (project in file("tools/gatling-test"))
  .enablePlugins(_root_.io.gatling.sbt.GatlingPlugin)
  .settings(gatlingCommonSettings)
  .settings(
    name := "reaction-gatling-test",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion,
      "io.gatling" % "gatling-test-framework" % gatlingVersion,
      "com.amazonaws" % "aws-java-sdk-core" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    ),
    publishArtifact in(GatlingIt, packageBin) := true
  )
  .settings(addArtifact(artifact in(GatlingIt, packageBin), packageBin in GatlingIt))

lazy val `gatling-runner` = (project in file("tools/gatling-runner"))
  .enablePlugins(JavaAppPackaging)
  .settings(gatlingCommonSettings)
  .settings(
    name := "thread-weaver-gatling-runner",
    libraryDependencies ++= Seq(
      "io.gatling" % "gatling-app" % gatlingVersion,
      "com.amazonaws" % "aws-java-sdk-core" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion
    ),
    mainClass in(Compile, bashScriptDefines) := Some("com.github.j5ik2o.gatling.runner.Runner"),
    dockerBaseImage := "openjdk:8",
    dockerUsername := Some("j5ik2o"),
    packageName in Docker := "thread-weaver-gatling-runner",
    dockerUpdateLatest := true,
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "mkdir /var/log/gatling"),
      Cmd("RUN", "chown daemon:daemon /var/log/gatling"),
      Cmd("ENV", "TW_GATLING_RESULT_DIR=/var/log/gatling")
    )
  )
  .dependsOn(
    `gatling-test` % "compile->gatling-it"
  )

val root = (project in file("."))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver"
  ).aggregate(`domain`, `use-case`, `interface`, `infrastructure`, `api`)