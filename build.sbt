val spinalVersion = "1.13.0"

lazy val root = (project in file("."))
  .settings(
    name         := "spinal-xilinx-reports",
    version      := "0.1.0",
    organization := "spinal.xilinx",
    scalaVersion := "2.13.12",
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" %% "spinalhdl-core"          % spinalVersion,
      "com.github.spinalhdl" %% "spinalhdl-lib"           % spinalVersion,
      compilerPlugin(
        "com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion
      ),
      "com.github.scopt"     %% "scopt"                   % "4.1.0",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
      "org.scalatest"        %% "scalatest"               % "3.2.17" % Test
    ),
    fork := true
  )

lazy val example = (project in file("example"))
  .dependsOn(root)
  .settings(
    name         := "spinal-xilinx-reports-example",
    scalaVersion := "2.13.12",
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
      "com.github.spinalhdl" %% "spinalhdl-lib"  % spinalVersion,
      compilerPlugin(
        "com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion
      )
    ),
    fork := true,
    Compile / mainClass := Some("ComplexMultExample")
  )
