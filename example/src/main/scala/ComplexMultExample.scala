import spinal.core.{Clock => _, _}
import spinal.lib._
import spinal.xilinx.reports._

// ── Multiplier8x8 ────────────────────────────────────────────────────────────
//
// Single-stage registered 8x8 multiplier, output truncated to 8 bits.
// Mirrors OCaml Hierarchical_design.Mult.create.

class Multiplier8x8 extends Component {
  val io = new Bundle {
    val clock = in Bool ()
    val clear = in Bool ()
    val a     = in  UInt(8 bits)
    val b     = in  UInt(8 bits)
    val o     = out UInt(8 bits)
  }
  noIoPrefix()

  val cd = ClockDomain(
    clock  = io.clock,
    reset  = io.clear,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = HIGH)
  )
  new ClockingArea(cd) {
    io.o := RegNext((io.a * io.b).resize(8))
  }
}

// ── ComplexMultiplier ─────────────────────────────────────────────────────────
//
// Computes (a_real + j*a_imag) * (b_real + j*b_imag) using four Multiplier8x8
// sub-components and a final register stage. Mirrors OCaml Hierarchical_design.create.
//
//   c_real = reg(rr - ii)   where rr = a_real*b_real, ii = a_imag*b_imag
//   c_imag = reg(ri + ir)   where ri = a_real*b_imag, ir = a_imag*b_real

class ComplexMultiplier extends Component {
  val io = new Bundle {
    val clock  = in Bool ()
    val clear  = in Bool ()
    val aReal  = in  UInt(8 bits)
    val aImag  = in  UInt(8 bits)
    val bReal  = in  UInt(8 bits)
    val bImag  = in  UInt(8 bits)
    val cReal  = out UInt(8 bits)
    val cImag  = out UInt(8 bits)
  }
  noIoPrefix()

  val cd = ClockDomain(
    clock  = io.clock,
    reset  = io.clear,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = HIGH)
  )

  private def mult(a: UInt, b: UInt): Multiplier8x8 = {
    val m = new Multiplier8x8
    m.io.clock := io.clock
    m.io.clear := io.clear
    m.io.a     := a
    m.io.b     := b
    m
  }

  val rr = mult(io.aReal, io.bReal)
  val ii = mult(io.aImag, io.bImag)
  val ri = mult(io.aReal, io.bImag)
  val ir = mult(io.aImag, io.bReal)

  new ClockingArea(cd) {
    io.cReal := RegNext((rr.io.o - ii.io.o).resize(8))
    io.cImag := RegNext((ri.io.o + ir.io.o).resize(8))
  }
}

// ── Entry point ───────────────────────────────────────────────────────────────
//
// Usage (dry-run, generates TCL/XDC/Verilog but does not call Vivado):
//
//   sbt "example/run --dir /tmp/mulme --part xcvu9p-flbg2104-2-e --clock clock:250"
//
// With Vivado (resource + timing estimates for top only):
//
//   sbt "example/run --dir /tmp/mulme --part xcvu9p-flbg2104-2-e --clock clock:250 --run"
//
// Full hierarchy synthesis (separate project per sub-module):
//
//   sbt "example/run --dir /tmp/mulme --part xcvu9p-flbg2104-2-e --clock clock:250 --hierarchy --run"

object ComplexMultExample {

  // The ordered factory list: top first, then sub-modules.
  // --hierarchy synthesizes all entries; without it only the first is used.
  val factories: Seq[(String, () => Component)] = Seq(
    "ComplexMultiplier" -> (() => new ComplexMultiplier),
    "Multiplier8x8"    -> (() => new Multiplier8x8)
  )

  def main(args: Array[String]): Unit =
    XilinxReports.parseArgs(args) match {
      case None        => sys.exit(1)
      case Some(flags) =>
        XilinxReports.run(
          factories  = factories,
          clocks     = flags.clocks,
          partName   = flags.partName,
          outputPath = flags.outputPath,
          flags      = flags
        )
    }
}
