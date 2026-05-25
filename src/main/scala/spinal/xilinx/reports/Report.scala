package spinal.xilinx.reports

import java.io.PrintStream
import scala.collection.mutable
import scala.io.Source

case class Subgroup   (name: String, value: Int)
case class Group      (name: String, value: Int, subgroups: Seq[Subgroup])
case class ClockTiming(name: String, setup: Double, hold: Double)
case class Report     (groups: Seq[Group], clocks: Seq[ClockTiming])

object Report {

  // ── Parsing ─────────────────────────────────────────────────────────

  private sealed trait ParsedLine
  private case class PGroup   (g: Group)        extends ParsedLine
  private case class PSubgroup(s: Subgroup)      extends ParsedLine
  private case class PTiming  (c: ClockTiming)   extends ParsedLine

  private def parseLine(line: String): ParsedLine =
    line.trim.split("\\s+").toList match {
      case "GROUP"    :: name :: value :: Nil      => PGroup(Group(name, value.toInt, Nil))
      case "SUBGROUP" :: name :: value :: Nil      => PSubgroup(Subgroup(name, value.toInt))
      case "TIMING"   :: name :: setup :: hold :: Nil =>
        PTiming(ClockTiming(name, setup.toDouble, hold.toDouble))
      case bad => sys.error(s"Could not parse report file line: '${bad.mkString(" ")}'")
    }

  def read(fileName: String): Report = {
    val src   = Source.fromFile(fileName)
    val lines = try src.getLines().map(parseLine).toSeq finally src.close()

    val groups    = lines.collect { case PGroup(g)    => g }
    val subgroups = lines.collect { case PSubgroup(s) => s }
    val clocks    = lines.collect { case PTiming(c)   => c }

    var groupMap = groups.map(g => g.name -> g).toMap
    subgroups.foreach { sub =>
      val groupName = sub.name.split(":")(0)
      groupMap.get(groupName).foreach { g =>
        groupMap = groupMap.updated(groupName, g.copy(subgroups = g.subgroups :+ sub))
      }
    }
    Report(groupMap.values.toSeq, clocks)
  }

  // ── Hierarchy traversal ──────────────────────────────────────────────

  // hierarchy: parentModuleName -> Seq[(instanceLabel, childModuleName)]
  private def listHierarchically(
    topLevelName: String,
    hierarchy:    Map[String, Seq[(String, String)]]
  ): Seq[(Int, Option[String], String)] = {
    val buf = mutable.Buffer.empty[(Int, Option[String], String)]
    def loop(level: Int, moduleName: String): Unit =
      hierarchy.getOrElse(moduleName, Nil).foreach { case (instLabel, childModule) =>
        buf += ((level + 1, Some(instLabel), childModule))
        loop(level + 1, childModule)
      }
    buf += ((0, None, topLevelName))
    loop(0, topLevelName)
    buf.toSeq
  }

  private def mkName(level: Int, moduleName: String, instanceLabel: Option[String]): String = {
    val indent = " " * level
    instanceLabel match {
      case None       => s"$indent$moduleName"
      case Some(inst) => s"$indent$moduleName (inst = $inst)"
    }
  }

  // ── Table rendering ──────────────────────────────────────────────────

  def printUtilizationTable(
    out:          PrintStream,
    topLevelName: String,
    hierarchy:    Map[String, Seq[(String, String)]],
    reports:      Seq[(String, Option[Report])]
  ): Unit = {
    val reportMap = reports.toMap
    val headerReport = reports.collectFirst { case (_, Some(r)) => r }
    val headerCols: Seq[String] = headerReport match {
      case None    => return
      case Some(r) =>
        r.groups.flatMap { g =>
          g.name +: g.subgroups.map(s => s.name.split(":")(1))
        }
    }
    if (headerCols.isEmpty) return

    val allRows = listHierarchically(topLevelName, hierarchy).map {
      case (level, instLabel, moduleName) =>
        val name = mkName(level, moduleName, instLabel)
        reportMap.get(moduleName).flatten match {
          case None    => name +: Seq.fill(headerCols.size)("-")
          case Some(r) =>
            val values = r.groups.flatMap(g => g.value.toString +: g.subgroups.map(_.value.toString))
            name +: values
        }
    }
    printAsciiTable(out, "-NAME" +: headerCols, allRows)
  }

  def printTimingTable(
    out:          PrintStream,
    topLevelName: String,
    hierarchy:    Map[String, Seq[(String, String)]],
    reports:      Seq[(String, Option[Report])]
  ): Unit = {
    val allClocks = reports.flatMap(_._2.toSeq.flatMap(_.clocks.map(_.name))).distinct.sorted
    if (allClocks.isEmpty) return

    val reportMap = reports.toMap
    val allRows = listHierarchically(topLevelName, hierarchy).map {
      case (level, instLabel, moduleName) =>
        val name = mkName(level, moduleName, instLabel)
        reportMap.get(moduleName).flatten match {
          case None    => name +: Seq.fill(allClocks.size)("-")
          case Some(r) =>
            val clockMap = r.clocks.map(c => c.name -> c).toMap
            name +: allClocks.map(cn => clockMap.get(cn).fold("-")(c => s"${c.setup}/${c.hold}"))
        }
    }
    printAsciiTable(out, "-NAME" +: allClocks, allRows)
  }

  // Fixed-width ASCII table. A header starting with '-' is left-aligned; others right-aligned.
  private def printAsciiTable(
    out:     PrintStream,
    headers: Seq[String],
    rows:    Seq[Seq[String]]
  ): Unit = {
    val n       = headers.size
    val padded  = rows.map(r => r ++ Seq.fill(n - r.size)(""))
    val widths  = (0 until n).map { i =>
      (headers(i).stripPrefix("-") +: padded.map(_(i))).map(_.length).max
    }
    val leftAlign = headers.map(_.startsWith("-"))

    def fmt(cells: Seq[String]) =
      cells.zip(widths).zip(leftAlign).map { case ((cell, w), left) =>
        if (left) cell.padTo(w, ' ') else s"%${w}s".format(cell)
      }.mkString(" | ")

    val divider = widths.map("-" * _).mkString("-+-")
    out.println(fmt(headers.map(_.stripPrefix("-"))))
    out.println(divider)
    padded.foreach(r => out.println(fmt(r)))
  }
}
