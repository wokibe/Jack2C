// simplified Logger
// kittekat Sep/2017

package net.kittenberger.wolfgang.mylogger

enum class Level { DEBUG, INFO, WARN, ERROR, FATAL }

class Logger(lvl: Level) {
  val level = lvl

  fun debug(txt: String) {
    if (level <= Level.DEBUG)
      log(Level.DEBUG, txt)
  }

  fun info(txt: String) {
    if (level <= Level.INFO)
      log(Level.INFO, txt)
  }

  fun warn(txt: String) {
    if (level <= Level.WARN)
      log(Level.WARN, txt)
  }

  fun error(txt: String) {
    if (level <= Level.ERROR)
      log(Level.ERROR, txt)
  }

  fun fatal(txt: String) {
    log(Level.FATAL, txt)
    System.exit(1)
  }

  private fun log(lvl: Level, txt: String) {
    System.err.println("${lvl.name[0]}/ $txt")
  }
}
