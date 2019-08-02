import Dependencies._
import Settings._
import Utils.RandomPortSupport
import com.typesafe.sbt.packager.docker._
import org.seasar.util.lang.StringUtil

import scala.concurrent.duration._

val dbDriver   = "com.mysql.jdbc.Driver"
val dbName     = "tw"
val dbUser     = "tw"
val dbPassword = "passwd"
val dbPort     = RandomPortSupport.temporaryServerPort()
val dbUrl      = s"jdbc:mysql://localhost:$dbPort/$dbName?useSSL=false"

val `infrastructure` = (project in file("modules/infrastructure"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-infrastructure",
    libraryDependencies ++= Seq(
        "org.slf4j"                  % "slf4j-api"             % "1.7.25",
        "de.huxhorn.sulky"           % "de.huxhorn.sulky.ulid" % "8.2.0",
        "io.circe"                   %% "circe-core"           % circeVersion,
        "io.circe"                   %% "circe-generic"        % circeVersion,
        "io.circe"                   %% "circe-parser"         % circeVersion,
        "com.github.julien-truffaut" %% "monocle-core"         % monocleVersion,
        "com.github.julien-truffaut" %% "monocle-macro"        % monocleVersion,
        "com.google.guava"           % "guava"                 % "27.1-jre"
      )
  )

val `domain` = (project in file("modules/domain"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-domain"
  ).dependsOn(`infrastructure`)

val `contract-use-case` = (project in file("contracts/contract-use-case"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-contract-use-case",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed"  % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
      )
  )
  .dependsOn(`domain`)

lazy val `contract-grpc-proto-interface` = (project in file("contracts/contract-grpc-proto-interface"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    name := "thread-weaver-contract-grpc-proto-interface"
  )

val `contract-http-proto-interface` = (project in file("contracts/contract-http-proto-interface"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-contract-http-proto-interface"
  )

val `contract-interface` = (project in file("contracts/contract-interface"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-contract-interface",
    libraryDependencies ++= Seq(
        "com.typesafe.akka"            %% "akka-actor-typed"     % akkaVersion,
        "javax.ws.rs"                  % "javax.ws.rs-api"       % "2.0.1",
        "com.github.swagger-akka-http" %% "swagger-akka-http"    % "2.0.2",
        "com.github.swagger-akka-http" %% "swagger-scala-module" % "2.0.3",
        "io.swagger.core.v3"           % "swagger-core"          % swaggerVersion,
        "io.swagger.core.v3"           % "swagger-annotations"   % swaggerVersion,
        "io.swagger.core.v3"           % "swagger-models"        % swaggerVersion,
        "io.swagger.core.v3"           % "swagger-jaxrs2"        % swaggerVersion
      ),
    libraryDependencies ++= Kamon.all
  )
  .dependsOn(`contract-use-case`, `contract-grpc-proto-interface`, `contract-http-proto-interface`)

val `use-case` = (project in file("modules/use-case"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-use-case",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed"            % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
      )
  ).dependsOn(`contract-use-case`, `contract-interface`, `domain`, `infrastructure`)

val `flyway` = (project in file("tools/flyway"))
  .enablePlugins(FlywayPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-flyway",
    libraryDependencies ++= Seq("mysql" % "mysql-connector-java" % "5.1.42"),
    parallelExecution in Test := false,
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(dbPort),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some(2 minutes),
    flywayDriver := dbDriver,
    flywayUrl := dbUrl,
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
        s"filesystem:${baseDirectory.value}/src/test/resources/db-migration/",
        s"filesystem:${baseDirectory.value}/src/test/resources/db-migration/test"
      ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
        "engineName"                 -> "MEMORY",
        "idSequenceNumberEngineName" -> "MyISAM"
      ),
    flywayMigrate := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val `api-client` = (project in file("api-client"))
  .dependsOn(`contract-grpc-proto-interface`, `contract-http-proto-interface`)
  .enablePlugins(AkkaGrpcPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-api-client",
    libraryDependencies ++= Seq(
        "de.heikoseeberger" %% "akka-http-circe" % "1.25.2"
      )
  ).dependsOn(`infrastructure`)

val interface = (project in file("modules/interface"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-interface",
    libraryDependencies ++= Seq(
        "mysql"                      % "mysql-connector-java"         % "5.1.42",
        "com.typesafe.slick"         %% "slick"                       % slickVersion,
        "com.typesafe.slick"         %% "slick-hikaricp"              % slickVersion,
        "com.typesafe.akka"          %% "akka-actor-typed"            % akkaVersion,
        "com.typesafe.akka"          %% "akka-stream-typed"           % akkaVersion,
        "com.typesafe.akka"          %% "akka-persistence-typed"      % akkaVersion,
        "com.typesafe.akka"          %% "akka-cluster-sharding-typed" % akkaVersion,
        "com.typesafe.akka"          %% "akka-actor"                  % akkaVersion,
        "com.typesafe.akka"          %% "akka-stream"                 % akkaVersion,
        "com.typesafe.akka"          %% "akka-persistence"            % akkaVersion,
        "com.typesafe.akka"          %% "akka-persistence-query"      % akkaVersion,
        "com.typesafe.akka"          %% "akka-cluster-tools"          % akkaVersion,
        "com.typesafe.akka"          %% "akka-cluster"                % akkaVersion,
        "com.typesafe.akka"          %% "akka-cluster-typed"          % akkaVersion,
        "com.typesafe.akka"          %% "akka-cluster-sharding"       % akkaVersion,
        "com.typesafe.akka"          %% "akka-slf4j"                  % akkaVersion,
        "com.typesafe.akka"          %% "akka-http"                   % akkaHttpVersion,
        "de.heikoseeberger"          %% "akka-http-circe"             % "1.25.2",
        "ch.megard"                  %% "akka-http-cors"              % "0.4.0",
        "com.github.j5ik2o"          %% "akka-persistence-dynamodb"   % "1.0.6-SNAPSHOT",
        "com.typesafe.akka"          %% "akka-testkit"                % akkaVersion % Test,
        "com.typesafe.akka"          %% "akka-actor-testkit-typed"    % akkaVersion % Test,
        "com.typesafe.akka"          %% "akka-multi-node-testkit"     % akkaVersion % Test,
        "com.typesafe.akka"          %% "akka-http"                   % akkaHttpVersion,
        "com.typesafe.akka"          %% "akka-http-testkit"           % akkaHttpVersion % Test,
        "com.github.julien-truffaut" %% "monocle-law"                 % monocleVersion % Test,
        "org.fusesource.leveldbjni"  % "leveldbjni-all"               % "1.8" % Test,
        "org.iq80.leveldb"           % "leveldb"                      % "0.9" % Test,
        "commons-io"                 % "commons-io"                   % "2.4" % Test,
        "com.github.j5ik2o"          %% "reactive-aws-dynamodb-core"  % "1.1.0" % Test,
        "com.github.j5ik2o"          %% "reactive-aws-dynamodb-test"  % "1.1.0" % Test,
        "com.github.j5ik2o"          %% "scalatestplus-db"            % "1.0.8" % Test,
        AspectJ.version
      ),
    // sbt-dao-generator
    // JDBCのドライバークラス名を指定します(必須)
    driverClassName in generator := dbDriver,
    // JDBCの接続URLを指定します(必須)
    jdbcUrl in generator := dbUrl,
    // JDBCの接続ユーザ名を指定します(必須)
    jdbcUser in generator := dbUser,
    // JDBCの接続ユーザのパスワードを指定します(必須)
    jdbcPassword in generator := dbPassword,
    // カラム型名をどのクラスにマッピングするかを決める関数を記述します(必須)
    propertyTypeNameMapper in generator := {
      case "INTEGER" | "TINYINT" | "INT"     => "Int"
      case "BIGINT"                          => "Long"
      case "VARCHAR"                         => "String"
      case "BOOLEAN" | "BIT"                 => "Boolean"
      case "DATE" | "TIMESTAMP" | "DATETIME" => "java.time.Instant"
      case "DECIMAL"                         => "BigDecimal"
      case "ENUM"                            => "String"
    },
    propertyNameMapper in generator := {
      case "type"     => "`type`"
      case columnName => StringUtil.decapitalize(StringUtil.camelize(columnName))
    },
    tableNameFilter in generator := { tableName: String =>
      tableName.toUpperCase match {
        case "SCHEMA_VERSION"                      => false
        case "FLYWAY_SCHEMA_HISTORY"               => false
        case t if t.endsWith("ID_SEQUENCE_NUMBER") => false
        case _                                     => true
      }
    },
    outputDirectoryMapper in generator := {
      case s if s.endsWith("Spec") => (sourceDirectory in Test).value
      case s =>
        new java.io.File((scalaSource in Compile).value, "/com/github/j5ik2o/threadWeaver/adaptor/dao/jdbc")
    },
    // モデル名に対してどのテンプレートを利用するか指定できます。
    templateNameMapper in generator := {
      case className if className.endsWith("Spec") => "template_spec.ftl"
      case _                                       => "template.ftl"
    },
    compile in Compile := ((compile in Compile) dependsOn (generateAll in generator)).value,
    generateAll in generator := Def
        .taskDyn {
          val ga = (generateAll in generator).value
          Def
            .task {
              (wixMySQLStop in flyway).value
            }
            .map(_ => ga)
        }
        .dependsOn(flywayMigrate in flyway)
        .value,
    // --- sbt-multi-jvm用の設定
    compile in MultiJvm := (compile in MultiJvm).triggeredBy(compile in Test).value,
    executeTests in Test := Def.task {
        val testResults      = (executeTests in Test).value
        val multiNodeResults = (executeTests in MultiJvm).value
        val overall = (testResults.overall, multiNodeResults.overall) match {
          case (TestResult.Passed, TestResult.Passed) => TestResult.Passed
          case (TestResult.Error, _)                  => TestResult.Error
          case (_, TestResult.Error)                  => TestResult.Error
          case (TestResult.Failed, _)                 => TestResult.Failed
          case (_, TestResult.Failed)                 => TestResult.Failed
        }
        Tests.Output(
          overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries
        )
      }.value,
    assemblyMergeStrategy in (MultiJvm, assembly) := {
      case "application.conf" => MergeStrategy.concat
      case "META-INF/aop.xml" => MergeStrategy.concat
      case x =>
        val old = (assemblyMergeStrategy in (MultiJvm, assembly)).value
        old(x)
    },
    Test / fork := true
    // , logLevel := Level.Debug
  )
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .dependsOn(`contract-interface`, `use-case`, `infrastructure`)

val `api-server` = (project in file("api-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(ecrSettings)
  .settings(
    name := "thread-weaver-api-server",
    mainClass in (Compile, run) := Some("com.github.j5ik2o.threadWeaver.api.Main"),
    mainClass in reStart := Some("com.github.j5ik2o.threadWeaver.api.Main"),
    dockerEntrypoint := Seq("/opt/docker/bin/thread-weaver-api-server"),
    dockerUsername := Some("j5ik2o"),
    fork in run := true,
    javaAgents += "org.aspectj"            % "aspectjweaver"    % "1.8.13",
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
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
        "com.github.scopt"     %% "scopt"                   % "4.0.0-RC2",
        "net.logstash.logback" % "logstash-logback-encoder" % "4.11" excludeAll (
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-core"),
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-databind")
        ),
        "com.lightbend.akka.management" %% "akka-management"                   % akkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http"      % akkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
        "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % akkaManagementVersion,
        "com.github.TanUkkii007"        %% "akka-cluster-custom-downing"       % "0.0.12",
        "com.github.everpeace"          %% "healthchecks-core"                 % "0.4.0",
        "com.github.everpeace"          %% "healthchecks-k8s-probes"           % "0.4.0",
        "org.slf4j"                     % "jul-to-slf4j"                       % "1.7.26",
        "ch.qos.logback"                % "logback-classic"                    % "1.2.3",
        "org.codehaus.janino"           % "janino"                             % "3.0.6"
      )
  ).dependsOn(`interface`)

val `read-model-updater` = (project in file("read-model-updater"))
  .enablePlugins(AshScriptPlugin, JavaAgent, EcrPlugin)
  .settings(baseSettings)
  .settings(dockerCommonSettings)
  .settings(ecrSettings)
  .settings(
    name := "thread-weaver-read-model-updater",
    mainClass in (Compile, run) := Some("com.github.j5ik2o.threadWeaver.rmu.Main"),
    mainClass in reStart := Some("com.github.j5ik2o.threadWeaver.rmu.Main"),
    dockerEntrypoint := Seq("/opt/docker/bin/thread-weaver-read-model-updater"),
    dockerUsername := Some("j5ik2o"),
    fork in run := true,
    javaAgents += "org.aspectj"            % "aspectjweaver"    % "1.8.13",
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
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
        "com.github.scopt"     %% "scopt"                   % "4.0.0-RC2",
        "net.logstash.logback" % "logstash-logback-encoder" % "4.11" excludeAll (
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-core"),
          ExclusionRule(organization = "com.fasterxml.jackson.core", name = "jackson-databind")
        ),
        "com.lightbend.akka.management" %% "akka-management"                   % akkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-http"      % akkaManagementVersion,
        "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
        "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % akkaManagementVersion,
        "com.github.TanUkkii007"        %% "akka-cluster-custom-downing"       % "0.0.12",
        "com.github.everpeace"          %% "healthchecks-core"                 % "0.4.0",
        "com.github.everpeace"          %% "healthchecks-k8s-probes"           % "0.4.0",
        "org.slf4j"                     % "jul-to-slf4j"                       % "1.7.26",
        "ch.qos.logback"                % "logback-classic"                    % "1.2.3",
        "org.codehaus.janino"           % "janino"                             % "3.0.6"
      )
  ).dependsOn(`interface`)

val dynamoDBLocalVersion = "1.11.477"
val sqlite4javaVersion   = "1.0.392"

lazy val `local-dynamodb` = (project in file("tools/local-dynamodb"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-local-dynamodb",
    libraryDependencies ++= Seq(
        "com.amazonaws"            % "DynamoDBLocal"               % dynamoDBLocalVersion,
        "com.almworks.sqlite4java" % "sqlite4java"                 % sqlite4javaVersion,
        "com.almworks.sqlite4java" % "sqlite4java-win32-x86"       % sqlite4javaVersion,
        "com.almworks.sqlite4java" % "sqlite4java-win32-x64"       % sqlite4javaVersion,
        "com.almworks.sqlite4java" % "libsqlite4java-osx"          % sqlite4javaVersion,
        "com.almworks.sqlite4java" % "libsqlite4java-linux-i386"   % sqlite4javaVersion,
        "com.almworks.sqlite4java" % "libsqlite4java-linux-amd64"  % sqlite4javaVersion,
        "com.github.j5ik2o"        %% "reactive-aws-dynamodb-core" % "1.1.0"
      )
  ).dependsOn(`migrate-dynamodb`)

lazy val `local-mysql` = (project in file("tools/local-mysql"))
  .enablePlugins(FlywayPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-local-mysql",
    libraryDependencies ++= Seq("mysql" % "mysql-connector-java" % "5.1.42"),
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(3306),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble),
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://localhost:3306/$dbName?useSSL=false",
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/",
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/test",
        s"filesystem:${baseDirectory.value}/src/main/resources/dummy-migration"
      ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
        "engineName"                 -> "InnoDB",
        "idSequenceNumberEngineName" -> "MyISAM"
      ),
    run := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val `migrate-mysql` = (project in file("tools/migrate-mysql"))
  .enablePlugins(FlywayPlugin)
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-migrate-mysql",
    libraryDependencies ++= Seq("mysql" % "mysql-connector-java" % "5.1.42"),
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://${scala.util.Properties
        .propOrNone("mysql.host")
        .getOrElse("localhost")}:${scala.util.Properties.propOrNone("mysql.port").getOrElse("3306")}/${scala.util.Properties
        .propOrNone("mysql.dbName").getOrElse(dbName)}?useSSL=false",
    flywayUser := scala.util.Properties.propOrNone("mysql.user").getOrElse(dbUser),
    flywayPassword := scala.util.Properties.propOrNone("mysql.password").getOrElse(dbPassword),
    flywaySchemas := scala.util.Properties
        .propOrNone("mysql.schemas").map(_.split(",").toSeq).getOrElse(
          Seq(scala.util.Properties.propOrNone("mysql.dbName").getOrElse(dbName))
        ),
    flywayLocations := {
      Seq(
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/",
        s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/db-migration/test"
      )
    },
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
        "engineName"                 -> "InnoDB",
        "idSequenceNumberEngineName" -> "MyISAM"
      ),
    run := flywayMigrate.value
  )

lazy val `migrate-dynamodb` = (project in file("tools/migrate-dynamodb"))
  .settings(baseSettings)
  .settings(
    name := "thread-weaver-migrate-dynamodb",
    libraryDependencies ++= Seq(
        "com.github.j5ik2o" %% "reactive-aws-dynamodb-core" % "1.1.0"
      )
  )

lazy val `gatling-test` = (project in file("tools/gatling-test"))
  .enablePlugins(GatlingPlugin)
  .settings(gatlingCommonSettings)
  .settings(
    name := "thread-weaver-gatling-test",
    libraryDependencies ++= Seq(
        "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion,
        "io.gatling"            % "gatling-test-framework"    % gatlingVersion,
        "com.amazonaws"         % "aws-java-sdk-core"         % awsSdkVersion,
        "com.amazonaws"         % "aws-java-sdk-s3"           % awsSdkVersion,
        "io.circe"              %% "circe-core"               % circeVersion,
        "io.circe"              %% "circe-generic"            % circeVersion,
        "io.circe"              %% "circe-parser"             % circeVersion
      ),
    publishArtifact in (GatlingIt, packageBin) := true
  )
  .settings(addArtifact(artifact in (GatlingIt, packageBin), packageBin in GatlingIt))
  .dependsOn(`infrastructure`, `contract-http-proto-interface`)

lazy val `gatling-s3-reporter` = (project in file("tools/gatling-s3-reporter"))
//  .settings(gatlingS3ReporterSettings)
  .settings(
    name := "thread-weaver-gatling-s3-reporter"
  )

lazy val `gatling-aggregate-runner` = (project in file("tools/gatling-aggregate-runner"))
  .enablePlugins(JavaAppPackaging, EcrPlugin)
  .settings(gatlingCommonSettings)
  .settings(gatlingAggregateRunnerEcrSettings)
  .settings(gatlingAggregateRunTaskSettings)
  .settings(
    name := "thread-weaver-gatling-aggregate-runner",
    mainClass in (Compile, bashScriptDefines) := Some("com.github.j5ik2o.gatling.runner.Runner"),
    dockerBaseImage := "openjdk:8",
    dockerUsername := Some("j5ik2o"),
    packageName in Docker := "thread-weaver-gatling-aggregate-runner",
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
        "org.slf4j"           % "slf4j-api"              % "1.7.26",
        "ch.qos.logback"      % "logback-classic"        % "1.2.3",
        "org.codehaus.janino" % "janino"                 % "3.0.6",
        "com.iheart"          %% "ficus"                 % "1.4.6",
        "com.github.j5ik2o"   %% "reactive-aws-ecs-core" % "1.1.3",
        "org.scalaj"          %% "scalaj-http"           % "2.4.2"
      )
  )

lazy val `gatling-runner` = (project in file("tools/gatling-runner"))
  .enablePlugins(JavaAppPackaging, EcrPlugin)
  .settings(gatlingCommonSettings)
  .settings(gatlingRunnerEcrSettings)
  .settings(
    name := "thread-weaver-gatling-runner",
    libraryDependencies ++= Seq(
        "io.gatling"    % "gatling-app"       % gatlingVersion,
        "com.amazonaws" % "aws-java-sdk-core" % awsSdkVersion,
        "com.amazonaws" % "aws-java-sdk-s3"   % awsSdkVersion
      ),
    mainClass in (Compile, bashScriptDefines) := Some("com.github.j5ik2o.gatling.runner.Runner"),
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
  ).aggregate(
    `domain`,
    `use-case`,
    `interface`,
    `infrastructure`,
    `contract-grpc-proto-interface`,
    `contract-http-proto-interface`,
    `api-client`,
    `api-server`
  )
