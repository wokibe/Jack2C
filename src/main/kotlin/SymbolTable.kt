// symbol table for jack2c
// kittekat oct/2017

package net.kittenberger.wolfgang.jack2c

enum class SymbolKind   { STATIC, FIELD, VAR, ARG, CLASS, CONST, UNKNOWN}
enum class Segments     { STATIC, THIS, LOCAL, ARGUMENT, CONSTANT}
data class SymbolEntry(val name: String,
                       val typ: String,
                       val seg: Segments,
                       val idx: Int)
typealias EntryArray = ArrayList< SymbolEntry >

// ==================================================================
class SymbolTable {
  private val classTable: HashMap< String, Int > = hashMapOf()
  private var classEntry: EntryArray = arrayListOf()

  private var localTable: HashMap< String, Int > = hashMapOf()
  private var localEntry: EntryArray = arrayListOf()

  private var staCount = 0
  private var fldCount = 0
  private var varCount = 0
  private var argCount = 0

/*
in the Jack2 VM space for variables is reserved in virtual segments
two symbol tables relates the defined names to their typ, segment and index

API:
the Generator fills these tables with:
  define(name, typ, kind)
    where
      kind      segment     table        description
      STATIC    STATIC      classTable   global (class) variables
      FIELD     THIS        classTable   method (instance) variables
      VAR       LOCAL       localTable   subroutine variables
      ARG       ARGUMENT    localTable   subroutine arguments

the readout of these tables is done with:
  seek(name).kind
  seek(name).typ
  seek(name).seg
  seek(name).idx

scope rules for the seek* functions:
  we scan first the localTable
  in case ther is nothing found we scan the  classTable

the Generator must clear the localTable at the begin of a subroutine

the Generator can retrive the *Count values with get*Count()

this Jack2c implementation allows named integer constants,
  both on the class and subroutine levels
  the Jack2 syntax is
    'const' identifier '=' ('-')? integerConstant
  both are implemented with an additiona virtual segment CONSTANT

the Generator uses the same call:
  define(name, value, kind)
    where
      kind      segment     table        description
      CLASS     CONSTANT    classTable   global constants
      CONST     CONSTANT    localTable   subroutine constants

  the readout of constants goes with
    seekIndex(name)

  negative constants are handled by the VMWriter with a 'neg' command

addressing of variables in the VM is done via "push/pop that <index>"
  for fields: the base address is returned from the constructor
  in methods the base address is delivered via "argument 0"
    and set via "pop pointer 0" to allow "push/pop this <Index>"

addressing of arrays:
  set the base address via "pop pointer 1" and then "push/pop that <index>"
*/

  // clear the local table at the begin of a new subroutine
  fun startSubroutine() {
    localTable = hashMapOf()
    localEntry = arrayListOf()
    varCount = 0
    argCount = 0
  }

  // add an entry into the correct table
  fun define(name: String, typ: String, kind: SymbolKind) {
    LOG.DEBUG("SymbolTable.define $name, $typ, $kind")
    when (kind) {
      SymbolKind.STATIC  ->  {
        if (classTable.containsKey(name))
          redefinition(name, "class")
        classTable[name] = classEntry.size
        classEntry.add(SymbolEntry(name, typ, Segments.STATIC, staCount))
        staCount++
      }

      SymbolKind.FIELD   ->  {
        if (classTable.containsKey(name))
          redefinition(name, "class")
        classTable[name] = classEntry.size
        classEntry.add(SymbolEntry(name, typ, Segments.THIS, fldCount))
        fldCount++
      }

      SymbolKind.VAR     ->  {
        if (localTable.containsKey(name))
          redefinition(name, "local")
        localTable[name] = localEntry.size
        localEntry.add(SymbolEntry(name, typ, Segments.LOCAL, varCount))
        varCount++
      }

      SymbolKind.ARG     ->  {
        if (localTable.containsKey(name))
          redefinition(name, "local")
        localTable[name] = localEntry.size
        localEntry.add(SymbolEntry(name, typ, Segments.ARGUMENT, argCount))
        argCount++
      }

      SymbolKind.CLASS  ->  {
        if (classTable.containsKey(name))
          redefinition(name, "class")
        classTable[name] = classEntry.size
        classEntry.add(SymbolEntry(name, "int", Segments.CONSTANT, typ.toInt()))
      }

      SymbolKind.CONST   ->  {
        if (localTable.containsKey(name))
          redefinition(name, "local")
        localTable[name] = localEntry.size
        localEntry.add(SymbolEntry(name, "int", Segments.CONSTANT, typ.toInt()))
      }

      else -> {}
    }
  }

  fun getStaCount() = staCount
  fun getFldCount() = fldCount
  fun getVarCount() = varCount
  fun getArgCount() = argCount

  fun seek(name: String): SymbolEntry? {
    if (localTable.containsKey(name))
      return localEntry[localTable[name]!!]
    if (classTable.containsKey(name))
      return classEntry[classTable[name]!!]
    // LOG.FATAL("SymbolTable/seek: unkown identifier $name")
    return null
  }

  // content of segment for inclusion in .vm file
  fun showTable(kind: SymbolKind): ArrayList< String > {
    var txt: ArrayList< String > = arrayListOf()
    when (kind) {
      SymbolKind.STATIC  ->  txt = showSeg(classEntry, Segments.STATIC)
      SymbolKind.FIELD   ->  txt = showSeg(classEntry, Segments.THIS)
      SymbolKind.VAR     ->  txt = showSeg(localEntry, Segments.LOCAL)
      SymbolKind.ARG     ->  txt = showSeg(localEntry, Segments.ARGUMENT)
      SymbolKind.CLASS   ->  txt = showSeg(classEntry, Segments.CONSTANT)
      SymbolKind.CONST   ->  txt = showSeg(localEntry, Segments.CONSTANT)
      else -> {}
    }
    return txt
  }

  // ==================================================================
  private fun redefinition(name: String, scope: String) {
    LOG.WARN("SymbolTable: $scope redefinition of $name" +
             " in ${FIL.clasName}/${FIL.subrName}")
  }

  private fun showSeg(entry: EntryArray, seg: Segments): ArrayList< String > {
    val txt: ArrayList< String > = arrayListOf()
    // LOG.DEBUG("SymbolTable.showSeg: seg = $seg")
    entry.forEach {
      // LOG.DEBUG("SymbolTable.showSeg: entry = $it")
      if (it.seg == seg)
        txt.add(("#S %-8s %6d %-12s %-8s").format(it.seg,
                                                    it.idx,
                                                    it.name,
                                                    it.typ))
    }
    return txt
  }
}
