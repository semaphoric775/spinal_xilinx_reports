package spinal.xilinx.reports

case class Clock(
  name:       String,
  period:     Double,
  clkSrcBufg: Option[String] = None
)

object Clock {
  def fromMhz(
    name:         String,
    frequencyMhz: Double,
    clkSrcBufg:   Option[String] = None
  ): Clock = Clock(name, 1000.0 / frequencyMhz, clkSrcBufg)

  def parseCliArg(s: String): Clock = s.split(":").toList match {
    case name :: freq :: Nil            => fromMhz(name, freq.toDouble)
    case name :: freq :: loc :: Nil     => fromMhz(name, freq.toDouble, Some(loc))
    case _                              => sys.error(s"Invalid clock spec '$s' — expected name:freq_mhz[:bufg_loc]")
  }
}
