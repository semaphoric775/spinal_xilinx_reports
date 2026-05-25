package spinal.xilinx.reports

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Ignore => ScalatestIgnore}
import spinal.core.{Clock => _, _}   // import all of spinal.core except Clock
import java.nio.file.Files

// Requires Vivado on PATH or VIVADO_PATH env var. Run manually.
// sbt "testOnly spinal.xilinx.reports.IntegrationSpec" with VIVADO_PATH set.
@ScalatestIgnore
class IntegrationSpec extends AnyFlatSpec with Matchers {

  class TrivialAndGate extends Component {
    val io = new Bundle {
      val clk = in Bool ()
      val a   = in  Bool()
      val b   = in  Bool()
      val y   = out Bool()
    }
    noIoPrefix()
    io.y := io.a & io.b
  }

  private val vivadoPath = sys.env.getOrElse("VIVADO_PATH", "vivado")

  "XilinxReports" should "run Vivado and produce a parseable report" in {
    val outDir = Files.createTempDirectory("spinal_integration_").toFile
    val clocks = Seq(Clock.fromMhz("clk", 250.0))
    val flags = CommandFlags(
      outputPath = outDir.getAbsolutePath,
      partName   = "xcvu9p-flbg2104-2-e",
      clocks     = clocks,
      run        = true,
      verbose    = false,
      pathToVivado = Some(vivadoPath)
    )

    XilinxReports.run(
      factories = Seq("TrivialAndGate" -> (() => new TrivialAndGate)),
      clocks    = clocks,
      partName  = flags.partName,
      outputPath = flags.outputPath,
      flags     = flags
    )
    val reportFile = s"${outDir.getAbsolutePath}/TrivialAndGate/post_synth_report.txt"
    val report = Report.read(reportFile)
    val clbCount = report.groups.find(_.name == "CLB").map(_.value).getOrElse(0)
    clbCount should be >= 0
  }

  it should "run Vivado on Spartan UltraScale+ and produce a parseable report" in {
    val outDir = Files.createTempDirectory("spinal_integration_spartanuplus_").toFile
    val clocks = Seq(Clock.fromMhz("clk", 100.0))
    val flags = CommandFlags(
      outputPath   = outDir.getAbsolutePath,
      partName     = "xcsu10p-cmva361-1-e",
      clocks       = clocks,
      run          = true,
      verbose      = false,
      pathToVivado = Some(vivadoPath),
      deviceFamily = DeviceFamily.SpartanUltraScalePlus
    )

    XilinxReports.run(
      factories  = Seq("TrivialAndGate" -> (() => new TrivialAndGate)),
      clocks     = clocks,
      partName   = flags.partName,
      outputPath = flags.outputPath,
      flags      = flags
    )
    val reportFile = s"${outDir.getAbsolutePath}/TrivialAndGate/post_synth_report.txt"
    val report = Report.read(reportFile)
    report.groups.map(_.name) should contain("CLB")
    val clbCount = report.groups.find(_.name == "CLB").map(_.value).getOrElse(-1)
    clbCount should be >= 0
  }

  it should "run Vivado on Spartan-7 and produce a parseable report with 7-series group names" in {
    val outDir = Files.createTempDirectory("spinal_integration_spartan7_").toFile
    val clocks = Seq(Clock.fromMhz("clk", 100.0))
    val flags = CommandFlags(
      outputPath   = outDir.getAbsolutePath,
      partName     = "xc7s50csga324-1",
      clocks       = clocks,
      run          = true,
      verbose      = false,
      pathToVivado = Some(vivadoPath),
      deviceFamily = DeviceFamily.Spartan7
    )

    XilinxReports.run(
      factories  = Seq("TrivialAndGate" -> (() => new TrivialAndGate)),
      clocks     = clocks,
      partName   = flags.partName,
      outputPath = flags.outputPath,
      flags      = flags
    )
    val reportFile = s"${outDir.getAbsolutePath}/TrivialAndGate/post_synth_report.txt"
    val report = Report.read(reportFile)
    report.groups.map(_.name) should contain("LUT")
    report.groups.map(_.name) should contain("FLOP_LATCH")
    val lutCount = report.groups.find(_.name == "LUT").map(_.value).getOrElse(-1)
    lutCount should be >= 0
  }
}
