package spinal.xilinx.reports

import spinal.core.{Component, SpinalConfig}
import java.io.{File, PrintStream}
import java.util.concurrent.Semaphore
import scala.collection.mutable
import scala.collection.parallel.CollectionConverters._
import scopt.OParser

case class CommandFlags(
  outputPath:               String          = "",
  partName:                 String          = "",
  generateVivadoReports:    Boolean         = false,
  fullDesignHierarchy:      Boolean         = false,
  clocks:                   Seq[Clock]      = Nil,
  run:                      Boolean         = false,
  place:                    Boolean         = false,
  route:                    Boolean         = false,
  checkpoint:               Boolean         = false,
  optDesign:                Option[Boolean] = None,
  hierarchy:                Boolean         = false,
  disableHierarchyInReport: Boolean         = false,
  disableRetiming:          Boolean         = false,
  verbose:                  Boolean         = false,
  pathToVivado:             Option[String]  = None,
  maxConcurrentJobs:        Int             = 8,
  preserveHierarchy:        Boolean         = false,
  deviceFamily:             DeviceFamily    = DeviceFamily.UltraScale
)

object XilinxReports {

  /** Elaborate the top factory and traverse the resulting component tree.
   *  Returns Map[parentDefinitionName -> Seq[(instanceName, childDefinitionName)]].
   *  Output is written to a temp directory and discarded.
   */
  def discoverHierarchy(topFactory: () => Component): Map[String, Seq[(String, String)]] = {
    val tmpDir = File.createTempFile("spinal_xilinx_discovery_", "")
    tmpDir.delete()
    tmpDir.mkdirs()
    try {
      val spinalCfg = SpinalConfig(targetDirectory = tmpDir.getAbsolutePath, verbose = false)
      val report = spinalCfg.generateVerilog(topFactory())
      buildHierarchyMap(report.toplevel)
    } finally {
      tmpDir.listFiles().foreach(_.delete())
      tmpDir.delete()
    }
  }

  private def buildHierarchyMap(top: Component): Map[String, Seq[(String, String)]] = {
    val result = mutable.Map.empty[String, mutable.Buffer[(String, String)]]
    def traverse(parent: Component): Unit =
      parent.children.foreach { child =>
        val buf = result.getOrElseUpdate(parent.definitionName, mutable.Buffer.empty)
        buf += ((child.getName(), child.definitionName))
        traverse(child)
      }
    traverse(top)
    result.view.mapValues(_.toSeq).toMap
  }

  /** Create one Vivado project per factory, optionally run Vivado, then print tables.
   *
   *  factories: Seq of (logicalName, factory). The first entry is the top-level module.
   *  When flags.hierarchy = false only the first factory is synthesized.
   *  When flags.hierarchy = true all provided factories are synthesized.
   */
  def run(
    factories:       Seq[(String, () => Component)],
    clocks:          Seq[Clock],
    partName:        String,
    outputPath:      String,
    flags:           CommandFlags        = CommandFlags(),
    primitiveGroups: Seq[PrimitiveGroup] = PrimitiveGroup.default,
    out:             PrintStream         = System.out
  ): Unit = {
    require(factories.nonEmpty, "At least one factory (the top-level) must be provided")

    val topLogicalName = factories.head._1

    val hierarchy = discoverHierarchy(factories.head._2)

    val resolvedGroups =
      if (primitiveGroups eq PrimitiveGroup.default) flags.deviceFamily.defaultPrimitiveGroups
      else primitiveGroups

    val config = ProjectConfig(
      vivadoUtilizationReport = flags.generateVivadoReports,
      vivadoTimingReport      = flags.generateVivadoReports,
      primitiveGroups         = resolvedGroups,
      fullDesignHierarchy     = flags.fullDesignHierarchy,
      optDesign               = flags.optDesign,
      reportHierarchy         = !flags.disableHierarchyInReport,
      retiming                = !flags.disableRetiming
    )

    val selectedFactories = if (flags.hierarchy) factories else factories.take(1)

    out.println(s"Creating Xilinx report projects in $outputPath")
    val projects: Seq[(String, ProjectHandle)] = selectedFactories.map { case (name, factory) =>
      val projectDir = s"$outputPath/$name"
      new File(projectDir).mkdirs()
      val handle = Project.create(
        factory           = factory,
        clocks            = clocks,
        partName          = partName,
        outputPath        = projectDir,
        config            = config,
        place             = flags.place,
        route             = flags.route,
        checkpoint        = flags.checkpoint,
        preserveHierarchy = flags.preserveHierarchy
      )
      (name, handle)
    }

    if (flags.run) {
      val vivado = flags.pathToVivado.getOrElse("vivado")
      val sem    = new Semaphore(flags.maxConcurrentJobs)

      val results: Seq[(String, Option[Report])] = projects.par.map { case (name, handle) =>
        sem.acquire()
        try {
          out.println(s"Running project for $name")
          val result = Project.run(handle, verbose = flags.verbose, pathToVivado = vivado)
          out.println(s"Completed project for $name")
          (name, result)
        } finally sem.release()
      }.seq

      projects.foreach { case (name, handle) =>
        out.println(s"Project for $name lives in ${handle.outputPath}")
      }
      Report.printUtilizationTable(out, topLogicalName, hierarchy, results)
      Report.printTimingTable(out, topLogicalName, hierarchy, results)
    }
  }

  // ── scopt CLI parser ──────────────────────────────────────────────────

  def parseArgs(args: Array[String]): Option[CommandFlags] = {
    val builder = scopt.OParser.builder[CommandFlags]
    val parser = {
      import builder._
      OParser.sequence(
        programName("spinal-xilinx-reports"),
        opt[String]('d', "dir")
          .required()
          .action((v, f) => f.copy(outputPath = v))
          .text("output directory for generated files"),
        opt[String]('p', "part")
          .required()
          .action((v, f) => f.copy(partName = v))
          .text("FPGA part name (e.g. xcvu9p-flbg2104-2-e)"),
        opt[String]("clock")
          .unbounded()
          .action((v, f) => f.copy(clocks = f.clocks :+ Clock.parseCliArg(v)))
          .text("clock constraint: name:freq_mhz[:bufg_loc]"),
        opt[Unit]("run")
          .action((_, f) => f.copy(run = true))
          .text("execute Vivado"),
        opt[Unit]("place")
          .action((_, f) => f.copy(place = true))
          .text("run placement after synthesis"),
        opt[Unit]("route")
          .action((_, f) => f.copy(route = true))
          .text("run routing after placement"),
        opt[Unit]("checkpoint")
          .action((_, f) => f.copy(checkpoint = true))
          .text("write checkpoints after each stage"),
        opt[Unit]("hierarchy")
          .action((_, f) => f.copy(hierarchy = true))
          .text("synthesize all provided sub-module factories"),
        opt[Boolean]("opt")
          .action((v, f) => f.copy(optDesign = Some(v)))
          .text("run opt_design pass (default: follows fullDesignHierarchy)"),
        opt[Unit]("full-hierarchy")
          .action((_, f) => f.copy(fullDesignHierarchy = true))
          .text("include full design hierarchy in synthesis"),
        opt[Unit]("disable-retiming")
          .action((_, f) => f.copy(disableRetiming = true))
          .text("disable synthesis retiming"),
        opt[Unit]("preserve-hierarchy")
          .action((_, f) => f.copy(preserveHierarchy = true))
          .text("pass -flatten_hierarchy none to synth_design"),
        opt[Unit]("disable-hierarchy-in-report")
          .action((_, f) => f.copy(disableHierarchyInReport = true))
          .text("do not use -hier in get_cells queries"),
        opt[Unit]("reports")
          .action((_, f) => f.copy(generateVivadoReports = true))
          .text("generate standard Vivado utilization/timing reports"),
        opt[Unit]("verbose")
          .action((_, f) => f.copy(verbose = true))
          .text("show Vivado output"),
        opt[String]("vivado")
          .action((v, f) => f.copy(pathToVivado = Some(v)))
          .text("path to Vivado binary (default: vivado)"),
        opt[Int]("jobs")
          .action((v, f) => f.copy(maxConcurrentJobs = v))
          .text("max concurrent Vivado jobs (default: 8)"),
        opt[String]("family")
          .action((v, f) => f.copy(deviceFamily = v match {
            case "ultrascale"    => DeviceFamily.UltraScale
            case "spartanuplus"  => DeviceFamily.SpartanUltraScalePlus
            case "spartan7"      => DeviceFamily.Spartan7
            case other           => throw new IllegalArgumentException(s"Unknown device family: $other (valid: ultrascale, spartanuplus, spartan7)")
          }))
          .text("device family for default primitive groups: ultrascale (default), spartanuplus, or spartan7")
      )
    }
    scopt.OParser.parse(parser, args, CommandFlags())
  }
}
