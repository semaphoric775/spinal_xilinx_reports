package spinal.xilinx.reports

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.{File, PrintWriter}
import java.nio.file.Files

class ReportParserSpec extends AnyFlatSpec with Matchers {

  private def writeReport(content: String): String = {
    val f = Files.createTempFile("report_test_", ".txt").toFile
    f.deleteOnExit()
    val pw = new PrintWriter(f)
    try pw.print(content) finally pw.close()
    f.getAbsolutePath
  }

  "Report.read" should "parse GROUP lines" in {
    val path = writeReport("GROUP CLB 42\nGROUP REGISTER 7\n")
    val r = Report.read(path)
    r.groups.map(_.name)  should contain("CLB")
    r.groups.find(_.name == "CLB").get.value shouldBe 42
    r.groups.find(_.name == "REGISTER").get.value shouldBe 7
  }

  it should "associate SUBGROUP lines under the correct parent GROUP" in {
    val path = writeReport(
      "GROUP CLB 50\n" +
      "SUBGROUP CLB:LUT 40\n" +
      "SUBGROUP CLB:LUTRAM 10\n"
    )
    val r = Report.read(path)
    val clb = r.groups.find(_.name == "CLB").get
    clb.subgroups.map(_.name) should contain("CLB:LUT")
    clb.subgroups.map(_.name) should contain("CLB:LUTRAM")
    clb.subgroups.find(_.name == "CLB:LUT").get.value    shouldBe 40
    clb.subgroups.find(_.name == "CLB:LUTRAM").get.value shouldBe 10
  }

  it should "parse TIMING lines" in {
    val path = writeReport("TIMING clk 0.523 0.301\n")
    val r = Report.read(path)
    r.clocks should have length 1
    r.clocks.head.name  shouldBe "clk"
    r.clocks.head.setup shouldBe 0.523 +- 1e-9
    r.clocks.head.hold  shouldBe 0.301 +- 1e-9
  }

  it should "handle multiple clocks" in {
    val path = writeReport(
      "TIMING clk_a 1.0 0.5\n" +
      "TIMING clk_b -0.2 0.1\n"
    )
    val r = Report.read(path)
    r.clocks.map(_.name) should contain allOf ("clk_a", "clk_b")
  }

  it should "handle mixed GROUP, SUBGROUP, TIMING in any order" in {
    val path = writeReport(
      "TIMING clk 0.1 0.2\n" +
      "GROUP CLB 10\n" +
      "SUBGROUP CLB:LUT 8\n"
    )
    val r = Report.read(path)
    r.groups should have length 1
    r.clocks should have length 1
    r.groups.head.subgroups should have length 1
  }
}
