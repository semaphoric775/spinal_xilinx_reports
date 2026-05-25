package spinal.xilinx.reports

trait PrimitiveSubgroup {
  def primitiveSubgroup: String
  def ignoreMacroPrimitives: Boolean
}

// ── CLB ──────────────────────────────────────────────────────────────

sealed abstract class ClbSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  def ignoreMacroPrimitives: Boolean = false
}
object ClbSubgroup {
  case object Latch  extends ClbSubgroup("LATCH")
  case object Carry  extends ClbSubgroup("CARRY")
  case object Lut    extends ClbSubgroup("LUT")
  case object Muxf   extends ClbSubgroup("MUXF")
  // Vivado expands LUTRAM MACROs into INTERNAL primitives — exclude MACRO level to avoid double-count
  case object Lutram extends ClbSubgroup("LUTRAM") { override val ignoreMacroPrimitives = true }
  case object Srl    extends ClbSubgroup("SRL")
  val all: Seq[ClbSubgroup] = Seq(Latch, Carry, Lut, Muxf, Lutram, Srl)
}

// ── Register ─────────────────────────────────────────────────────────

sealed abstract class RegisterSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object RegisterSubgroup {
  case object Sdr           extends RegisterSubgroup("SDR")
  case object Metastability extends RegisterSubgroup("METASTABILITY")
  case object Ddr           extends RegisterSubgroup("DDR")
  case object Latch         extends RegisterSubgroup("LATCH")
  val all: Seq[RegisterSubgroup] = Seq(Sdr, Metastability, Ddr, Latch)
}

// ── Blockram ─────────────────────────────────────────────────────────

sealed abstract class BlockramSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object BlockramSubgroup {
  case object Fifo extends BlockramSubgroup("FIFO")
  case object Bram extends BlockramSubgroup("BRAM")
  case object Uram extends BlockramSubgroup("URAM")
  val all: Seq[BlockramSubgroup] = Seq(Fifo, Bram, Uram)
}

// ── Advanced ─────────────────────────────────────────────────────────

sealed abstract class AdvancedSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object AdvancedSubgroup {
  case object Mac        extends AdvancedSubgroup("MAC")
  case object Gt         extends AdvancedSubgroup("GT")
  case object Interlaken extends AdvancedSubgroup("INTERLAKEN")
  case object Pcie       extends AdvancedSubgroup("PCIE")
  case object Sysmon     extends AdvancedSubgroup("SYSMON")
  val all: Seq[AdvancedSubgroup] = Seq(Mac, Gt, Interlaken, Pcie, Sysmon)
}

// ── Arithmetic ───────────────────────────────────────────────────────

sealed abstract class ArithmeticSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object ArithmeticSubgroup {
  case object Dsp extends ArithmeticSubgroup("DSP")
  val all: Seq[ArithmeticSubgroup] = Seq(Dsp)
}

// ── Clock ────────────────────────────────────────────────────────────

sealed abstract class ClockSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object ClockSubgroup {
  case object Buffer extends ClockSubgroup("BUFFER")
  case object Mux    extends ClockSubgroup("MUX")
  case object Pll    extends ClockSubgroup("PLL")
  val all: Seq[ClockSubgroup] = Seq(Buffer, Mux, Pll)
}

// ── Configuration ────────────────────────────────────────────────────

sealed abstract class ConfigurationSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object ConfigurationSubgroup {
  case object Bscan      extends ConfigurationSubgroup("BSCAN")
  case object Dna        extends ConfigurationSubgroup("DNA")
  case object Efuse      extends ConfigurationSubgroup("EFUSE")
  case object Ecc        extends ConfigurationSubgroup("ECC")
  case object Icap       extends ConfigurationSubgroup("ICAP")
  case object MasterJtag extends ConfigurationSubgroup("MASTER_JTAG")
  case object Startup    extends ConfigurationSubgroup("STARTUP")
  val all: Seq[ConfigurationSubgroup] = Seq(Bscan, Dna, Efuse, Ecc, Icap, MasterJtag, Startup)
}

// ── IO ───────────────────────────────────────────────────────────────

sealed abstract class IoSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object IoSubgroup {
  case object Bitslice    extends IoSubgroup("BITSLICE")
  case object DciReset    extends IoSubgroup("DCI_RESET")
  case object InputBuffer extends IoSubgroup("INPUT_BUFFER")
  case object Delay       extends IoSubgroup("DELAY")
  case object BidirBuffer extends IoSubgroup("BIDIR_BUFFER")
  case object Serdes      extends IoSubgroup("SERDES")
  case object WeakDriver  extends IoSubgroup("WEAK_DRIVER")
  case object OutputBuffer extends IoSubgroup("OUTPUT_BUFFER")
  case object Sdr         extends IoSubgroup("SDR")
  case object Metastability extends IoSubgroup("METASTABILITY")
  case object Ddr         extends IoSubgroup("DDR")
  case object Latch       extends IoSubgroup("LATCH")
  val all: Seq[IoSubgroup] = Seq(
    Bitslice, DciReset, InputBuffer, Delay, BidirBuffer, Serdes,
    WeakDriver, OutputBuffer, Sdr, Metastability, Ddr, Latch
  )
}

// ── FlopLatch (7-series REGISTER equivalent) ─────────────────────────

sealed abstract class FlopLatchSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object FlopLatchSubgroup {
  case object Flop  extends FlopLatchSubgroup("flop")
  case object Latch extends FlopLatchSubgroup("latch")
  val all: Seq[FlopLatchSubgroup] = Seq(Flop, Latch)
}

// ── Bmem (7-series BLOCKRAM equivalent) ──────────────────────────────

sealed abstract class BmemSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object BmemSubgroup {
  case object Bram extends BmemSubgroup("bram")
  case object Fifo extends BmemSubgroup("fifo")
  val all: Seq[BmemSubgroup] = Seq(Bram, Fifo)
}

// ── Dmem (7-series distributed RAM / LUTRAM equivalent) ──────────────

sealed abstract class DmemSubgroup(val primitiveSubgroup: String) extends PrimitiveSubgroup {
  val ignoreMacroPrimitives = false
}
object DmemSubgroup {
  case object Dram extends DmemSubgroup("dram")
  val all: Seq[DmemSubgroup] = Seq(Dram)
}

// ── Top-level group ADT ──────────────────────────────────────────────

sealed trait PrimitiveGroup {
  def primitiveGroup: String
  def subgroups: Seq[PrimitiveSubgroup]
}

object PrimitiveGroup {
  // UltraScale / UltraScale+ groups
  case class Advanced     (subgroups: Seq[AdvancedSubgroup])       extends PrimitiveGroup { val primitiveGroup = "ADVANCED" }
  case class Arithmetic   (subgroups: Seq[ArithmeticSubgroup])     extends PrimitiveGroup { val primitiveGroup = "ARITHMETIC" }
  case class Blockram     (subgroups: Seq[BlockramSubgroup])       extends PrimitiveGroup { val primitiveGroup = "BLOCKRAM" }
  case class Clb          (subgroups: Seq[ClbSubgroup])            extends PrimitiveGroup { val primitiveGroup = "CLB" }
  case class ClockGroup   (subgroups: Seq[ClockSubgroup])          extends PrimitiveGroup { val primitiveGroup = "CLOCK" }
  case class Configuration(subgroups: Seq[ConfigurationSubgroup])  extends PrimitiveGroup { val primitiveGroup = "CONFIGURATION" }
  case class Io           (subgroups: Seq[IoSubgroup])             extends PrimitiveGroup { val primitiveGroup = "IO" }
  case class Register     (subgroups: Seq[RegisterSubgroup])       extends PrimitiveGroup { val primitiveGroup = "REGISTER" }

  // 7-series groups (different PRIMITIVE_GROUP strings from UltraScale)
  // LUT and CARRY are top-level groups on 7-series; PRIMITIVE_SUBGROUP is "others" for both
  case object Lut7   extends PrimitiveGroup { val primitiveGroup = "LUT";   val subgroups = Nil }
  case object Carry7 extends PrimitiveGroup { val primitiveGroup = "CARRY"; val subgroups = Nil }
  case class FlopLatch(subgroups: Seq[FlopLatchSubgroup]) extends PrimitiveGroup { val primitiveGroup = "FLOP_LATCH" }
  case class Bmem     (subgroups: Seq[BmemSubgroup])      extends PrimitiveGroup { val primitiveGroup = "BMEM" }
  case class Dmem     (subgroups: Seq[DmemSubgroup])      extends PrimitiveGroup { val primitiveGroup = "DMEM" }

  val default: Seq[PrimitiveGroup] = Seq(
    Clb(Seq(ClbSubgroup.Lut, ClbSubgroup.Muxf, ClbSubgroup.Carry, ClbSubgroup.Lutram, ClbSubgroup.Srl)),
    Register(Seq(RegisterSubgroup.Sdr)),
    Blockram(Seq(BlockramSubgroup.Fifo, BlockramSubgroup.Bram, BlockramSubgroup.Uram))
  )
}
