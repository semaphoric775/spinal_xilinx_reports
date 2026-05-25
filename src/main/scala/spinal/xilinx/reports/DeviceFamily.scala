package spinal.xilinx.reports

sealed trait DeviceFamily {
  def defaultPrimitiveGroups: Seq[PrimitiveGroup]
}

object DeviceFamily {
  case object UltraScale extends DeviceFamily {
    val defaultPrimitiveGroups: Seq[PrimitiveGroup] = PrimitiveGroup.default
  }

  // Spartan UltraScale+ (xcsu... parts) — same architecture as UltraScale+, same primitive names
  case object SpartanUltraScalePlus extends DeviceFamily {
    val defaultPrimitiveGroups: Seq[PrimitiveGroup] = PrimitiveGroup.default
  }

  // Spartan-7 / Artix-7 / Kintex-7 / Virtex-7 (7-series)
  // 7-series uses completely different PRIMITIVE_GROUP names from UltraScale:
  //   CLB/LUT    → LUT   (PRIMITIVE_SUBGROUP == "others", no useful sub-filter)
  //   CLB/CARRY  → CARRY (PRIMITIVE_SUBGROUP == "others", no useful sub-filter)
  //   REGISTER   → FLOP_LATCH / "flop"
  //   BLOCKRAM   → BMEM / "bram" | "fifo"
  //   CLB/LUTRAM → DMEM / "dram"
  case object Spartan7 extends DeviceFamily {
    val defaultPrimitiveGroups: Seq[PrimitiveGroup] = Seq(
      PrimitiveGroup.Lut7,
      PrimitiveGroup.FlopLatch(Seq(FlopLatchSubgroup.Flop)),
      PrimitiveGroup.Bmem(Seq(BmemSubgroup.Bram, BmemSubgroup.Fifo)),
      PrimitiveGroup.Dmem(Seq(DmemSubgroup.Dram))
    )
  }
}
