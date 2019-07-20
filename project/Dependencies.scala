import sbt._

object Dependencies {

  object AspectJ {
    val version = "org.aspectj" % "aspectjweaver" % "1.8.13"
  }

  object Kamon {

    val version = "0.6.7"

    val core = "io.kamon" %% "kamon-core" % version excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val autoweave = "io.kamon" %% "kamon-autoweave" % "0.6.5" excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val systemMetrics = "io.kamon" %% "kamon-system-metrics" % version excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val scala = "io.kamon" %% "kamon-scala" % version excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val akka = "io.kamon" %% "kamon-akka-2.5" % version excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules"),
      ExclusionRule(organization = "com.typesafe.akka")
    )

    val akkaHttp = "io.kamon" %% "kamon-akka-http" % "0.6.8" excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules"),
      ExclusionRule(organization = "com.typesafe.akka")
    )

    val datadog = "io.kamon" %% "kamon-datadog" % version excludeAll (
      ExclusionRule(organization = "org.asynchttpclient"), // awsclientのものとバッティングするため
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val jmx = "io.kamon" %% "kamon-jmx" % version excludeAll (
      ExclusionRule(organization = "log4j"),
      ExclusionRule(organization = "org.slf4j"),
      ExclusionRule(organization = "org.scala-lang.modules")
    )

    val all = Seq(core, autoweave, systemMetrics, scala, akka, akkaHttp, datadog, jmx)
  }

}
