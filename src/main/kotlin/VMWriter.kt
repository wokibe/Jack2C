// write vm code to .vm file
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==================================================================
class VMWriter(path: String) {

  companion object {
    var size = 0
  }

  val vmf = File(path)

  init {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val formatted = current.format(formatter)
    vmf.writeText("// $path generated at $formatted\n")
  }

  fun push(cmd: String, int: Int) {
    write("push %s %d".format(cmd, int))
  }

  fun pop(cmd: String, int: Int) {
    write("pop %s %d".format(cmd, int))
  }

  fun subroutine(className: String, subName: String, nrVars: Int) {
    write("function %s.%s %d".format(className, subName, nrVars))
  }

  fun call(className: String, subName: String, nrArgs: Int) {
    write("call %s.%s %d".format(className, subName, nrArgs))
  }

  fun arithmetic(op: String) {
    when (op) {
      "+" -> write("add")
      "-" -> write("sub")
      "*" -> write("call Math.multiply 2")
      "/" -> write("call Math.divide 2")
      "&" -> write("and")
      "|" -> write("or")
      "<" -> write("lt")
      ">" -> write("gt")
      "=" -> write("eq")
      else -> LOG.ERROR("VMWriter.arithmetic: unknown operator $op")
    }
  }

  fun unaryOp(op: String) {
    when (op) {
      "-" -> write("neg")
      "~" -> write("not")
      else -> LOG.ERROR("VMWriter.unaryOp: unknown operator $op")
    }
  }

  fun returnCmd() {
    write("return")
  }

  fun constant(int: Int) {
    push("constant", Math.abs(int))
    if (int < 0)
      write("neg")
  }

  fun goto(label: String) {
    write("goto " + label)
  }

  fun ifGoto(label: String) {
    write("if-goto " + label)
  }

  fun label(label: String) {
    write("label " + label)
  }

  fun comment(txt: String) {
    if (OPT.JACK)
      write("// $txt")
  }

  fun symbol(txt: String) {
    if (OPT.SYMB)
      write("// $txt")
  }

  // ==================================================================
  private fun write(txt: String) {
    vmf.appendText("$txt\n")
    VMWriter.size++
    //LOG.DEBUG("VMWriter: $txt")
  }
}
