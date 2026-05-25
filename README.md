# spinal-xilinx-reports

Scala/SpinalHDL port of Jane Street's [`hardcaml_xilinx_reports`](https://github.com/janestreet/hardcaml_xilinx_reports). Given a SpinalHDL component hierarchy, it generates per-module Vivado synthesis projects, runs Vivado in batch mode, and prints ASCII resource utilization and timing tables.

## Requirements

- sbt
- Vivado (on `PATH` or specified with `--vivado`)
- A UltraScale or UltraScale+ part (e.g. `xcsu10p-cmva361-1-e`). 7-series parts use different primitive group names and are not supported.

## Library usage

Provide a sequence of `(name, factory)` pairs — top module first, sub-modules after. The library elaborates the top factory to discover hierarchy for the table, then synthesizes each factory independently.

```scala
import spinal.xilinx.reports._

val factories: Seq[(String, () => Component)] = Seq(
  "MyTop"    -> (() => new MyTop),
  "SubBlock" -> (() => new SubBlock)
)

val clocks = Seq(Clock.fromMhz("clk", 250.0))

XilinxReports.run(
  factories  = factories,
  clocks     = clocks,
  partName   = "xcsu10p-cmva361-1-e",
  outputPath = "/tmp/my_reports",
  flags      = CommandFlags(run = true, hierarchy = true)
)
```

### Clock naming

Clock names must match the port names on the component exactly. Call `noIoPrefix()` inside any component whose clock port names need to match — SpinalHDL adds an `io_` prefix to bundle ports by default, which would break clock filtering.

```scala
class MyTop extends Component {
  val io = new Bundle {
    val clk = in Bool()
    ...
  }
  noIoPrefix()  // port is "clk", not "io_clk"
  ...
}
```

## CLI usage (example project)

The `example/` subproject (`ComplexMultExample`) demonstrates a two-level hierarchy: a `ComplexMultiplier` built from four `Multiplier8x8` sub-components.

### Dry run — generate TCL/XDC/Verilog, do not call Vivado

```
sbt "example/run --dir /tmp/out --part xcsu10p-cmva361-1-e --clock clock:250"
```

### Synthesize top module only

```
sbt "example/run --dir /tmp/out --part xcsu10p-cmva361-1-e --clock clock:250 --run"
```

### Synthesize all modules in the hierarchy

```
sbt "example/run --dir /tmp/out --part xcsu10p-cmva361-1-e --clock clock:250 --hierarchy --run"
```

Example output:

```
NAME                       | CLB | LUT | MUXF | CARRY | LUTRAM | SRL | REGISTER | SDR | BLOCKRAM | FIFO | BRAM | URAM
---------------------------+-----+-----+------+-------+--------+-----+----------+-----+----------+------+------+-----
ComplexMultiplier          | 178 | 164 |    0 |    14 |      0 |   0 |       48 |  48 |        0 |    0 |    0 |    0
 Multiplier8x8 (inst = rr) |  42 |  39 |    0 |     3 |      0 |   0 |        8 |   8 |        0 |    0 |    0 |    0
 Multiplier8x8 (inst = ii) |  42 |  39 |    0 |     3 |      0 |   0 |        8 |   8 |        0 |    0 |    0 |    0
 Multiplier8x8 (inst = ri) |  42 |  39 |    0 |     3 |      0 |   0 |        8 |   8 |        0 |    0 |    0 |    0
 Multiplier8x8 (inst = ir) |  42 |  39 |    0 |     3 |      0 |   0 |        8 |   8 |        0 |    0 |    0 |    0

NAME                       |       clock
---------------------------+------------
ComplexMultiplier          | 3.317/0.069
 Multiplier8x8 (inst = rr) |     0.0/0.0
 Multiplier8x8 (inst = ii) |     0.0/0.0
 Multiplier8x8 (inst = ri) |     0.0/0.0
 Multiplier8x8 (inst = ir) |     0.0/0.0
```

Timing values are `setup_slack/hold_slack` in nanoseconds. Leaf modules synthesized in isolation show `0.0/0.0` because there are no register-to-register paths without surrounding context.

## All CLI flags

| Flag | Description |
|------|-------------|
| `-d, --dir DIR` | Output directory for generated files (required) |
| `-p, --part PART` | FPGA part name (required) |
| `--clock name:freq_mhz[:bufg_loc]` | Clock constraint; repeat for multiple clocks |
| `--run` | Execute Vivado synthesis |
| `--hierarchy` | Synthesize all provided factories, not just the first |
| `--place` | Run `place_design` after synthesis |
| `--route` | Run `route_design` after placement |
| `--checkpoint` | Write `.dcp` checkpoints after each stage |
| `--preserve-hierarchy` | Pass `-flatten_hierarchy none` to `synth_design` |
| `--disable-retiming` | Disable synthesis retiming |
| `--reports` | Generate standard Vivado utilization/timing reports |
| `--vivado PATH` | Path to Vivado binary (default: `vivado`) |
| `--jobs N` | Max concurrent Vivado jobs (default: 8) |
| `--verbose` | Show Vivado output |

## Running tests

```
sbt test
```

The integration test (`IntegrationSpec`) is disabled by default. To run it with Vivado:

```
VIVADO_PATH=/path/to/vivado sbt "testOnly spinal.xilinx.reports.IntegrationSpec"
```
