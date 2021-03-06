import BuildHelper._
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import sbtcrossproject.CrossPlugin.autoImport.crossProject

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.github.io/zio-cache/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    ),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/zio-cache/"), "scm:git:git@github.com:zio/zio-cache.git")
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

val zioVersion = "1.0.0"

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    zioCacheJVM,
    zioCacheJS
  )

lazy val zioCache = crossProject(JSPlatform, JVMPlatform)
  .in(file("zio-cache"))
  .settings(stdSettings("zioCache"))
  .settings(crossProjectSettings)
  .settings(buildInfoSettings("zio.cache"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-streams"  % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    )
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val zioCacheJS = zioCache.js
  .settings(scalaJSUseMainModuleInitializer := true)

lazy val zioCacheJVM = zioCache.jvm
  .settings(dottySettings)

lazy val zioCacheBenchmarks = project
  .in(file("benchmarks"))
  .dependsOn(zioCacheJVM)
  .enablePlugins(JmhPlugin)
  .settings(buildInfoSettings("zio.cache"))
  
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= libraryDependencies.value
      .map(moduleid => moduleFilter(organization = moduleid.organization, name = moduleid.name))
      .reduce(_ | _),
    scalacOptions in Compile in console := Seq(
      "-Ypartial-unification",
      "-language:higherKinds",
      "-language:existentials",
      "-Yno-adapted-args",
      "-Xsource:2.13",
      "-Yrepl-class-based"
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "com.github.cb372" % "scalacache-core_2.12"     % "0.28.0",
      "com.github.cb372" % "scalacache-caffeine_2.12" % "0.28.0",
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    )
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val docs = project
  .in(file("zio-cache-docs"))
  .settings(
    skip.in(publish) := true,
    moduleName := "zio-cache-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion
    ),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(root),
    target in (ScalaUnidoc, unidoc) := (baseDirectory in LocalRootProject).value / "website" / "static" / "api",
    cleanFiles += (target in (ScalaUnidoc, unidoc)).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(unidoc in Compile).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(unidoc in Compile).value
  )
  .dependsOn(root)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
