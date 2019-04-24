val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"
val akkaManagementVersion = "1.0.0"
val circeVersion = "0.11.1"
val monocleVersion = "1.5.0"

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
  wartremoverErrors in(Compile, compile) ++= Seq(Wart.ArrayEquals, Wart.AnyVal, Wart.Var, Wart.Null, Wart.OptionPartial),
  resolvers += Resolver.bintrayRepo("tanukkii007", "maven"),
  libraryDependencies ++= Seq(
    "eu.timepit" %% "refined" % "0.9.5",
    "org.wvlet.airframe" %% "airframe" % "19.4.1",
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
  ),
  dependencyOverrides ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
    "com.typesafe.akka" %% "akka-actor" % "2.5.22",
    "com.typesafe.akka" %% "akka-stream" % "2.5.22",
    "com.typesafe.akka" %% "akka-cluster" % "2.5.22",
    "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.22",
    "com.typesafe.akka" %% "akka-discovery" % "2.5.22",
    "com.typesafe.akka" %% "akka-http" % "10.1.8",
    "com.typesafe.akka" %% "akka-http-core" % "10.1.8",
    "com.typesafe.akka" %% "akka-parsing" % "10.1.8"
  ),
  parallelExecution in Test := false,
  fork := true
)

val infrastructure = (project in file("infrastructure"))
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

val domain = (project in file("domain"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-domain"
  ).dependsOn(infrastructure)

val useCase = (project in file("use-case"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-use-case"
  ).dependsOn(domain, infrastructure)

val interface = (project in file("interface"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-interface",
    libraryDependencies ++= Seq(
      "io.kamon" %% "kamon-core" % "1.1.3",
      "io.kamon" %% "kamon-system-metrics" % "1.0.1",
      "io.kamon" %% "kamon-akka-2.5" % "1.0.0",
      "io.kamon" %% "kamon-akka-http-2.5" % "1.0.0",
      "io.kamon" %% "kamon-jmx-collector" % "0.1.8",
      "io.kamon" %% "kamon-datadog" % "1.0.0",
      "io.kamon" %% "kamon-logback" % "1.0.2",
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
      "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
      "com.github.TanUkkii007" %% "akka-cluster-custom-downing" % "0.0.12",
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.0.2",
      "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
      "ch.megard" %% "akka-http-cors" % "0.4.0",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" % Test,
      "org.iq80.leveldb" % "leveldb" % "0.9" % Test,
      "commons-io" % "commons-io" % "2.4" % Test,
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
    }
    //, logLevel := Level.Debug
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .dependsOn(useCase, infrastructure)

val api = (project in file("api"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-api",
    dockerBaseImage := "openjdk:8",
    dockerUsername := Some("j5ik2o")
  ).dependsOn(interface)

val root = (project in file("."))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver"
  ).aggregate(domain, useCase, interface, infrastructure, api)
