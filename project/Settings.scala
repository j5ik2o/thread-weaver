import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import wartremover.WartRemover.autoImport._

object Settings {
  val akkaVersion           = "2.5.22"
  val akkaHttpVersion       = "10.1.8"
  val akkaManagementVersion = "1.0.0"
  val circeVersion          = "0.11.1"
  val monocleVersion        = "1.5.0"
  val swaggerVersion        = "2.0.8"

  lazy val dockerCommonSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u191-b12",
    maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
    packageName in Docker := s"j5ik2o/${name.value}",
    dockerUpdateLatest := true,
    bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}"
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
    wartremoverErrors in (Compile, compile) ++= Seq(Wart.ArrayEquals,
                                                    Wart.AnyVal,
                                                    Wart.Var,
                                                    Wart.Null,
                                                    Wart.OptionPartial),
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.bintrayRepo("tanukkii007", "maven"),
      Resolver.bintrayRepo("kamon-io", "snapshots")
    ),
    libraryDependencies ++= Seq(
      "io.monix"           %% "monix"                % "3.0.0-RC2",
      "eu.timepit"         %% "refined"              % "0.9.5",
      "org.wvlet.airframe" %% "airframe"             % "19.4.1",
      "io.kamon"           %% "kamon-core"           % "1.1.6",
      "io.kamon"           %% "kamon-system-metrics" % "1.0.1",
      "io.kamon"           %% "kamon-akka-2.5"       % "1.0.0",
      "io.kamon"           %% "kamon-akka-http-2.5"  % "1.0.0",
      "io.kamon"           %% "kamon-jmx-collector"  % "0.1.8",
      "io.kamon"           %% "kamon-datadog"        % "1.0.0",
      "io.kamon"           %% "kamon-logback"        % "1.0.2",
      "io.kamon"           %% "kamon-scala-future"   % "1.0.0",
      "org.scalatest"      %% "scalatest"            % "3.0.4" % Test,
      "ch.qos.logback"     % "logback-classic"       % "1.2.3" % Test
    ),
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-xml"             % "1.2.0",
      "com.typesafe.akka"      %% "akka-actor"            % "2.5.22",
      "com.typesafe.akka"      %% "akka-stream"           % "2.5.22",
      "com.typesafe.akka"      %% "akka-cluster"          % "2.5.22",
      "com.typesafe.akka"      %% "akka-cluster-sharding" % "2.5.22",
      "com.typesafe.akka"      %% "akka-discovery"        % "2.5.22",
      "com.typesafe.akka"      %% "akka-http"             % "10.1.8",
      "com.typesafe.akka"      %% "akka-http-core"        % "10.1.8",
      "com.typesafe.akka"      %% "akka-parsing"          % "10.1.8"
    ),
    parallelExecution in Test := false,
    fork := true
  )

  lazy val gatlingCommonSettings = Seq(
    organization := "com.github.j5ik2o",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.11.8",
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

}
