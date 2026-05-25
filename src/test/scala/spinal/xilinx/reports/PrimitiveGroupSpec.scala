package spinal.xilinx.reports

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PrimitiveGroupSpec extends AnyFlatSpec with Matchers {

  "ClbSubgroup.Lutram" should "have ignoreMacroPrimitives = true" in {
    ClbSubgroup.Lutram.ignoreMacroPrimitives shouldBe true
  }

  it should "have primitiveSubgroup = LUTRAM" in {
    ClbSubgroup.Lutram.primitiveSubgroup shouldBe "LUTRAM"
  }

  "All other CLB subgroups" should "have ignoreMacroPrimitives = false" in {
    val others = Seq(ClbSubgroup.Latch, ClbSubgroup.Carry, ClbSubgroup.Lut,
                     ClbSubgroup.Muxf, ClbSubgroup.Srl)
    others.foreach(sg => sg.ignoreMacroPrimitives shouldBe false)
  }

  "PrimitiveGroup.Clb" should "report primitiveGroup = CLB" in {
    PrimitiveGroup.Clb(Nil).primitiveGroup shouldBe "CLB"
  }

  "PrimitiveGroup variants" should "return correct Vivado group names" in {
    PrimitiveGroup.Register(Nil).primitiveGroup      shouldBe "REGISTER"
    PrimitiveGroup.Blockram(Nil).primitiveGroup      shouldBe "BLOCKRAM"
    PrimitiveGroup.Advanced(Nil).primitiveGroup      shouldBe "ADVANCED"
    PrimitiveGroup.Arithmetic(Nil).primitiveGroup    shouldBe "ARITHMETIC"
    PrimitiveGroup.ClockGroup(Nil).primitiveGroup    shouldBe "CLOCK"
    PrimitiveGroup.Configuration(Nil).primitiveGroup shouldBe "CONFIGURATION"
    PrimitiveGroup.Io(Nil).primitiveGroup            shouldBe "IO"
  }

  "PrimitiveGroup.default" should "include CLB, REGISTER, and BLOCKRAM" in {
    val names = PrimitiveGroup.default.map(_.primitiveGroup)
    names should contain("CLB")
    names should contain("REGISTER")
    names should contain("BLOCKRAM")
  }

  "PrimitiveGroup.default CLB" should "include LUTRAM subgroup" in {
    val clb = PrimitiveGroup.default.collectFirst { case c: PrimitiveGroup.Clb => c }.get
    clb.subgroups.map(_.primitiveSubgroup) should contain("LUTRAM")
  }

  "DeviceFamily.UltraScale" should "include URAM in default blockram groups" in {
    val bram = DeviceFamily.UltraScale.defaultPrimitiveGroups
      .collectFirst { case b: PrimitiveGroup.Blockram => b }.get
    bram.subgroups.map(_.primitiveSubgroup) should contain("URAM")
  }

  "DeviceFamily.SpartanUltraScalePlus" should "use same default groups as UltraScale" in {
    DeviceFamily.SpartanUltraScalePlus.defaultPrimitiveGroups shouldEqual PrimitiveGroup.default
  }

  "DeviceFamily.Spartan7" should "use 7-series PRIMITIVE_GROUP names" in {
    val groups = DeviceFamily.Spartan7.defaultPrimitiveGroups.map(_.primitiveGroup)
    groups should contain("LUT")
    groups should contain("FLOP_LATCH")
    groups should contain("BMEM")
    groups should contain("DMEM")
  }

  it should "not reference UltraScale-only group names" in {
    val groups = DeviceFamily.Spartan7.defaultPrimitiveGroups.map(_.primitiveGroup)
    groups should not contain "CLB"
    groups should not contain "REGISTER"
    groups should not contain "BLOCKRAM"
  }

  it should "use lowercase subgroup names for FLOP_LATCH" in {
    val fl = DeviceFamily.Spartan7.defaultPrimitiveGroups
      .collectFirst { case f: PrimitiveGroup.FlopLatch => f }.get
    fl.subgroups.map(_.primitiveSubgroup) should contain("flop")
  }

  it should "use lowercase subgroup names for BMEM" in {
    val bmem = DeviceFamily.Spartan7.defaultPrimitiveGroups
      .collectFirst { case b: PrimitiveGroup.Bmem => b }.get
    bmem.subgroups.map(_.primitiveSubgroup) should contain("bram")
    bmem.subgroups.map(_.primitiveSubgroup) should contain("fifo")
  }

  it should "include DMEM with dram subgroup" in {
    val dmem = DeviceFamily.Spartan7.defaultPrimitiveGroups
      .collectFirst { case d: PrimitiveGroup.Dmem => d }.get
    dmem.subgroups.map(_.primitiveSubgroup) should contain("dram")
  }
}
