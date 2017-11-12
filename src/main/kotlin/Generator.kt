// code generation for jack2c
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

class InvalidAstInfoKey(message: String) : Exception(message)
class UndefinedIdentifier(message: String) : Exception(message)
class InvalidIndexing(message: String) : Exception(message)

// ==================================================================
class Generator() {
  private val sym = SymbolTable()
  private var lbl = Labels()
  private var className = ""
  private var subName = ""
  private var subTyp = ""
  private var subKind = ""
  private lateinit var lastAst: AstTree     // kept for hash access error catch
  private lateinit var lastKey: String      // kept for hash access error catch
  private lateinit var vmw: VMWriter

  // start with the root of the AST, which describes a class
  fun doClass(ast: AstTree) {
    try {
      initVMW()
      LOG.DEBUG("Generator.doClass ast: $ast")
      className = astInfo(ast,"name")
      FIL.clasName = className
      // vmw.comment("class $className")
      showLine(ast)
      distribute(ast)
    }
    catch (e: InvalidAstInfoKey)    { LOG.FATAL(e.message ?: "") }
    catch (e: UndefinedIdentifier)  { LOG.FATAL(e.message ?: "") }
    catch (e: InvalidIndexing)      { LOG.FATAL(e.message ?: "") }
  }

  // ==================================================================
  private fun initVMW() {
    val path = FIL.basePath + "/vm"
    checkDir(path)
    val vmFile = path + "/" + FIL.baseName + ".vm"
    LOG.DEBUG("vmFile: $vmFile")
    vmw = VMWriter(vmFile)
  }

  // loop over all leafs of this ast
  private fun distribute(ast: AstTree) {
    ast.leafs.forEach {
      doAst(it)
    }
  }

  // execute the method correspondig to this ast
  private fun doAst(ast: AstTree) {
    lastAst = ast
    LOG.DEBUG("Generator.doAst: $ast")
    when (ast.typ) {
      ASTTYP.ClassVarConst    -> doClassVarConst(ast)
      ASTTYP.ClassVarDecl     -> doClassVarDecl(ast)
      ASTTYP.ConstDecl        -> doConstDecl(ast)
      ASTTYP.Subroutines      -> doSubroutines(ast)
      ASTTYP.SubroutineDecl   -> doSubroutineDecl(ast)
      ASTTYP.ParameterList    -> doParameterList(ast)
      ASTTYP.ParameterDecl    -> doParameterDecl(ast)
      ASTTYP.SubroutineBody   -> doSubroutineBody(ast)
      ASTTYP.VarConstDecl     -> doVarConstDecl(ast)
      ASTTYP.VarDecl          -> doVarDecl(ast)
      ASTTYP.Statements       -> doStatements(ast)
      ASTTYP.LetStatement     -> doLetStatement(ast)
      ASTTYP.IfStatement      -> doIfStatement(ast)
      ASTTYP.WhileStatement   -> doWhileStatement(ast)
      ASTTYP.BreakStatement   -> doBreakStatement(ast)
      ASTTYP.DoStatement      -> doDoStatement(ast)
      ASTTYP.ReturnStatement  -> doReturnStatement(ast)
      ASTTYP.SubroutineCall   -> doSubroutineCall(ast)
      ASTTYP.ExpressionList   -> doExpressionList(ast)
      ASTTYP.Expression       -> doExpression(ast)
      ASTTYP.Term             -> doTerm(ast)
      ASTTYP.Constant         -> doConstant(ast)
      ASTTYP.Condition        -> doCondition(ast)
      ASTTYP.IfWhileBlock     -> doIfWhileBlock(ast)
      ASTTYP.ElseBlock        -> doElseBlock(ast)
      ASTTYP.ArrayIndex       -> doArrayIndex(ast)
      else -> LOG.FATAL("Generator.doAst: unknown typ: ${ast.typ}")
    }
  }

  // execute only if the typ of the ast is the expected typ
  private fun doAstIf(ast: AstTree, expected: ASTTYP) {
    if (ast.typ == expected)
      doAst(ast)
    else {
      LOG.ERROR("doAstIf: unexpected ASTTYP: ${ast.typ} / expected: $expected")
    }
  }

  private fun doClassVarConst(ast: AstTree) {
    LOG.DEBUG("Generator.doClassVarConst $ast")
    distribute(ast)
    showClassTables()
  }

  private fun doClassVarDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doClassVarDecl $ast")
    val idList = astInfo(ast, "ids")
    val typ = astInfo(ast, "typ")
    val ids = idList.split(",")
    val kind = kindToSymbolKind(astInfo(ast, "kind"))
    ids.forEach {
      LOG.DEBUG("Generator.doClassVarDecl $it $typ $kind")
      sym.define(it, typ, kind)
    }
  }

  private fun doConstDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doConstDecl $ast")
    val id = astInfo(ast, "id")
    val value = astInfo(ast, "value")
    val kind = kindToSymbolKind(astInfo(ast, "kind"))
    sym.define(id, value, kind)
  }

  private fun doSubroutines(ast: AstTree) {
    LOG.DEBUG("Generator.doSubroutines $ast")
    distribute(ast)
  }

  private fun doSubroutineDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doSubroutineDecl $ast")
    subKind = astInfo(ast, "kind")
    subTyp = astInfo(ast, "typ")
    subName = astInfo(ast, "name")
    FIL.subrName = subName
    // vmw.comment("#########################################################")
    // vmw.comment("%s %s %s".format(subKind, subTyp, subName))
    showLine(ast)
    sym.startSubroutine()
    if (subKind == "method")  // reserve argument space for pointer to fields
      sym.define("this", className, SymbolKind.ARG)
    distribute(ast)
  }

  private fun doParameterList(ast: AstTree) {
    LOG.DEBUG("Generator.doParameterList $ast")
    distribute(ast)
  }

  private fun doParameterDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doParameterDecl $ast")
    val name = astInfo(ast, "name")
    val typ = astInfo(ast, "typ")
    sym.define(name, typ, SymbolKind.ARG)
  }

  private fun doSubroutineBody(ast: AstTree) {
    LOG.DEBUG("Generator.doSubroutineBody $ast")
    distribute(ast)
  }

  private fun doVarConstDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doVarConstDecl $ast")
    distribute(ast)
    // all declaretions done
    doSubroutineHeader()
  }

  private fun doVarDecl(ast: AstTree) {
    LOG.DEBUG("Generator.doVarDecl $ast")
    val idList = astInfo(ast, "ids")
    val typ = astInfo(ast, "typ")
    val ids = idList.split(",")
    ids.forEach {
      sym.define(it, typ, SymbolKind.VAR)
    }
  }

  private fun doSubroutineHeader() {
    vmw.subroutine(className, subName, sym.getVarCount())
    showLocalTables()
    lbl = Labels()

    // allocate space for fields
    if (subKind == "constructor") {
      val noFields = sym.getFldCount()
      if (OPT.ENHC)
        vmw.comment("#E allocate space for fields")
      vmw.constant(noFields)
      vmw.call("Memory", "alloc", 1)
      vmw.pop("pointer", 0)
    }

    // in methods "argument 0" must be moved to "pointer 0" ("this" adressing)
    if (subKind == "method") {
      if (OPT.ENHC)
        vmw.comment("#E set pointer to fields")
      vmw.push("argument", 0)
      vmw.pop("pointer", 0)
    }
  }

  private fun doStatements(ast: AstTree) {
    LOG.DEBUG("Generator.doStataements $ast")
    distribute(ast)
  }

  private fun doLetStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doLetStatement $ast")
    showLine(ast)
    val id = astInfo(ast, "id")
    val symEntry = symSeek(id, ast)
    val typ = symEntry.typ
    val seg = symEntry.seg.name.toLowerCase()
    val idx = symEntry.idx

    fun writeIndexed() {
      doAddressArray(ast, id, seg, typ, idx)
      if (OPT.ENHC)
        vmw.comment("#E calculate value")
      doAstIf(ast.leafs[1], ASTTYP.Expression)
      if (OPT.ENHC)
        vmw.comment("#E store into array")
      vmw.pop("temp", 0)
      vmw.pop("pointer", 1)
      vmw.push("temp", 0)
      vmw.pop("that", 0)
    }

    fun writeDirect() {
      doAst(ast.leafs[0])
      vmw.pop(seg, idx)
    }

    if (ast.leafs[0].typ == ASTTYP.ArrayIndex)
      writeIndexed()
    else
      writeDirect()
  }

  private fun doIfStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doIfStatement $ast")
    showLine(ast)
    lbl.pushIf("$className.$subName")
    doAstIf(ast.leafs[0], ASTTYP.Condition)
    vmw.unaryOp("~")                // test for false
    vmw.ifGoto(lbl.getIfEls())
    doAstIf(ast.leafs[1], ASTTYP.IfWhileBlock)
    vmw.goto(lbl.getIfEnd())
    vmw.label(lbl.getIfEls())
    if (ast.leafs.size > 2)
      doAstIf(ast.leafs[2], ASTTYP.ElseBlock)
    vmw.label(lbl.getIfEnd())
    lbl.popIf()
  }

  private fun doWhileStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doWhileStatement $ast")
    showLine(ast)
    lbl.pushWhile("$className.$subName")
    vmw.label(lbl.getWhileExp())
    doAstIf(ast.leafs[0], ASTTYP.Condition)
    vmw.unaryOp("~")                // test for false
    vmw.ifGoto(lbl.getWhileEnd())
    if (ast.leafs.size > 1)
      doAstIf(ast.leafs[1], ASTTYP.IfWhileBlock)
    vmw.goto(lbl.getWhileExp())
    vmw.label(lbl.getWhileEnd())
    lbl.popWhile()
  }

  private fun doBreakStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doIfStatement $ast")
    showLine(ast)
    if (lbl.isWhileEmpty())
      LOG.ERROR(invalidBreak(ast))
    else
      vmw.ifGoto(lbl.getWhileEnd())
  }

  private fun doDoStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doDoStatement $ast")
    showLine(ast)
    doAstIf(ast.leafs[0], ASTTYP.SubroutineCall)
    if (OPT.ENHC)
      vmw.comment("#E discard the void return")
    vmw.pop("temp", 0)
    }

  private fun doReturnStatement(ast: AstTree) {
    LOG.DEBUG("Generator.doReturnStatement $ast")
    showLine(ast)
    val case = astInfo(ast, "case")
    if (case == "void") {
      if (subTyp == "void")
        vmw.constant(0)   // push dummy return resul
      else
        LOG.ERROR(missingReturnExpr(ast))
    }
    else
      doAstIf(ast.leafs[0], ASTTYP.Expression)
    vmw.returnCmd()
  }

  // ==================================================================
  private fun doAddressArray(ast: AstTree,
                            id: String, seg: String, typ: String, idx: Int) {
    if (!(typ == "Array"))
      invalidIndexing(ast, id)
    if (OPT.ENHC)
      vmw.comment("#E calculate array index")
    doAstIf(ast.leafs[0], ASTTYP.ArrayIndex)
    if (OPT.ENHC)
      vmw.comment("#E add base address")
    vmw.push(seg, idx)
    vmw.arithmetic("+")
  }

  private fun doSubroutineCall(ast: AstTree) {
    LOG.DEBUG("Generator.doSubroutineCall $ast")
    val id = astInfo(ast, "id")
    var klass: String
    var name: String
    var nrParams = 0
    if (astLook(ast, "name")) {   // call format id.name(...)
      if (symLook(id)) {        // id is a variables
        if (OPT.ENHC)
          vmw.comment("#E remote method call")
        val symEntry = symSeek(id, ast)
        val seg = symEntry.seg.name.toLowerCase()
        val idx = symEntry.idx
        if (OPT.ENHC)
          vmw.comment("#E set arg 0 with remote object")
        vmw.push(seg, idx)
        nrParams = 1
        klass = symEntry.typ
      } else {
        if (OPT.ENHC)
          vmw.comment("#E remote function call")
        klass = id
      }
      name = astInfo(ast, "name")
    } else {                      // local method call
      if (OPT.ENHC)
        vmw.comment("#E local method call")
      klass = className
      name = id
      if (OPT.ENHC)
        vmw.comment("#E set arg 0 with local object")
      vmw.push("pointer", 0)
      nrParams = 1
    }
    // collect list of actual ParameterList
    doAstIf(ast.leafs[0], ASTTYP.ExpressionList)
    nrParams = nrParams + astInfo(ast.leafs[0], "size").toInt()
    vmw.call(klass, name, nrParams)
  }

  private fun doExpressionList(ast: AstTree) {
    LOG.DEBUG("Generator.doExpressionList $ast")
    distribute(ast)
  }

  private fun doExpression(ast: AstTree) {
    LOG.DEBUG("Generator.doExpression $ast")
    val ops = astInfo(ast, "ops")
    val opsList = ops.split("")
    val opsQueue = Queue<String>()
    var op: String
    opsList.forEach {
      if (it != "")
        opsQueue.enqueue(it)
    }
    if (ast.leafs.size > 0) {
      doTerm(ast.leafs[0])
      (1..(ast.leafs.size - 1)).forEach {
        doTerm(ast.leafs[it])
        op = opsQueue.dequeue()!!
        vmw.arithmetic(op)
      }
    }
  }

  private fun doTerm(ast: AstTree) {
    LOG.DEBUG("Generator.doTerm $ast")

      fun doUnary(ast: AstTree) {
        doAst(ast.leafs[0])
        vmw.unaryOp(astInfo(ast,"unaryOp"))
      }
      fun doArray(ast: AstTree) {
        val id = astInfo(ast, "id")
        val symEntry = symSeek(id, ast)
        val typ = symEntry.typ
        val seg = symEntry.seg.name.toLowerCase()
        val idx = symEntry.idx
        doAddressArray(ast, id, seg, typ, idx)
        if (OPT.ENHC)
          vmw.comment("#E read from array")
        vmw.pop("pointer", 1)
        vmw.push("that", 0)
      }
      fun doId(ast: AstTree) {
        val id = astInfo(ast, "id")
        val symEntry = symSeek(id, ast)
        val seg = symEntry.seg.name.toLowerCase()
        val idx = symEntry.idx
        if (seg == "constant")
          vmw.constant(idx)
        else
          vmw.push(seg, idx)
      }

    when (astInfo(ast, "case")) {
      "const",
      "expr",
      "subr"      -> doAst(ast.leafs[0])
      "unary"     -> doUnary(ast)
      "array"     -> doArray(ast)
      "id"        -> doId(ast)
    }
  }

  private fun doConstant(ast: AstTree) {
    LOG.DEBUG("Generator.doConstant $ast")
    val typ = astInfo(ast, "typ")
    val cnst = astInfo(ast, "const")

    fun doInteger(cnst: String) {
      if (cnst.toInt() > 32767)
        LOG.ERROR("integer constant greater than 32767")
      vmw.constant(cnst.toInt())
    }
    fun doCharConst(cnst: String) {
      vmw.constant(cnst.get(0).toInt())
    }
    fun doKeyWordConst(cnst: String) {
      when (cnst) {
        "null",
        "false"  -> vmw.constant(0)
        "true"   -> vmw.constant(-1)
        "this"   -> vmw.push("pointer", 0)
      }
    }
    fun doString(cnst: String) {
      val len = cnst.length
      vmw.constant(len)
      vmw.call("String", "new", 1)
      cnst.forEach {
        vmw.constant(it.toInt())
        vmw.call("String", "appendChar", 2)
      }
    }

    when (typ) {
      "INTEGER"           -> doInteger(cnst)
      "CHARCONSTANT"      -> doCharConst(cnst)
      "KEYWORDCONSTANT"   -> doKeyWordConst(cnst)
      "STRING"            -> doString(cnst)
    }
  }

  private fun doCondition(ast: AstTree) {
    LOG.DEBUG("Generator.doCondition $ast")
    doAstIf(ast.leafs[0], ASTTYP.Expression)
  }

  private fun doIfWhileBlock(ast: AstTree) {
    LOG.DEBUG("Generator.doIfWhileBlock $ast")
    doAstIf(ast.leafs[0], ASTTYP.Statements)
  }

  private fun doElseBlock(ast: AstTree) {
    LOG.DEBUG("Generator.doElseBlock $ast")
    doAstIf(ast.leafs[0], ASTTYP.Statements)
  }

  private fun doArrayIndex(ast: AstTree) {
    LOG.DEBUG("Generator.doArrayIndex $ast")
    doAstIf(ast.leafs[0], ASTTYP.Expression)
  }

  private fun kindToSymbolKind(kind: String): SymbolKind {
    val symbolKind = when (kind) {
      "static"    -> SymbolKind.STATIC
      "field"     -> SymbolKind.FIELD
      "var"       -> SymbolKind.VAR
      "arg"       -> SymbolKind.ARG
      "class"     -> SymbolKind.CLASS
      "const"     -> SymbolKind.CONST
      else        -> SymbolKind.UNKNOWN
    }
    return symbolKind
  }

  private fun showClassTables() {
    val static = sym.showTable(SymbolKind.STATIC)
    static.forEach {
      vmw.symbol(it)
    }
    val field = sym.showTable(SymbolKind.FIELD)
    field.forEach {
      vmw.symbol(it)
    }
    val cnst = sym.showTable(SymbolKind.CLASS)
    cnst.forEach {
      vmw.symbol(it)
    }
  }

  private fun showLocalTables() {
    val vars = sym.showTable(SymbolKind.VAR)
    vars.forEach {
      vmw.symbol(it)
    }
    val args = sym.showTable(SymbolKind.ARG)
    args.forEach {
      vmw.symbol(it)
    }
    val consts = sym.showTable(SymbolKind.CONST)
    consts.forEach {
      vmw.symbol(it)
    }
  }

  private fun astInfo(ast: AstTree, key: String): String =
    ast.info[key] ?: invalidAstInfoKey(ast, key)

  private fun astLook(ast: AstTree, key: String) =
    ast.info[key] != null

  private fun symSeek(id: String, ast: AstTree): SymbolEntry =
    sym.seek(id) ?: undefinedIdentifier(ast, id)

  private fun symLook(id: String) = sym.seek(id) != null

  private fun invalidAstInfoKey(ast: AstTree, key: String): Nothing {
    val stack = Throwable().stackTrace
    var txt: ArrayList<String> = arrayListOf()
    txt.add("Invalid AstTree.info[\"$key\"] access" +
            " while generating $className.$subName")
    txt.add("   ast: $ast")
    txt.add("   Called from ${stack[2]}")
    throw InvalidAstInfoKey(txt.joinToString("\n"))
  }

  private fun undefinedIdentifier(ast: AstTree, id: String): Nothing {
    var txt: ArrayList<String> = arrayListOf()
    val row = astInfo(ast, "row")
    val col = astInfo(ast, "col")
    txt.add("undefined identifier $id in \"$subName\" " +
      "(file: ${FIL.fileName} [$row/$col])")
    val showPos = showErrorPos(row.toInt(), col.toInt())
    showPos.forEach {
      txt.add(it)
    }
    throw UndefinedIdentifier(txt.joinToString("\n"))
  }

  private fun invalidIndexing(ast: AstTree, id: String): Nothing {
    var txt: ArrayList<String> = arrayListOf()
    val row = astInfo(ast, "row")
    val col = astInfo(ast, "col")
    txt.add("invalid indexing of $id in \"$subName\"." +
      " only allowed for arrays (file: ${FIL.fileName} [$row/$col])")
    val showPos = showErrorPos(row.toInt(), col.toInt())
    showPos.forEach {
      txt.add(it)
    }
    throw InvalidIndexing(txt.joinToString("\n"))
  }

  private fun invalidBreak(ast: AstTree): String {
    var txt: ArrayList<String> = arrayListOf()
    val row = astInfo(ast, "row")
    val col = astInfo(ast, "col")
    txt.add("invalid invalid 'break' usage in \"$subName\"." +
      " only allowed in a 'while' context (file: ${FIL.fileName} [$row/$col])")
    val showPos = showErrorPos(row.toInt(), col.toInt())
    showPos.forEach {
      txt.add(it)
    }
    return txt.joinToString("\n")
  }

  private fun missingReturnExpr(ast: AstTree): String {
    var txt: ArrayList<String> = arrayListOf()
    val row = astInfo(ast, "row")
    val col = astInfo(ast, "col")
    txt.add("missing 'return' expression in \"$subName\"." +
      " (file: ${FIL.fileName} [$row/$col])")
    val showPos = showErrorPos(row.toInt(), col.toInt())
    showPos.forEach {
      txt.add(it)
    }
    return txt.joinToString("\n")
  }

  private fun invalidThisReturn(ast: AstTree): String {
    var txt: ArrayList<String> = arrayListOf()
    val row = astInfo(ast, "row")
    val col = astInfo(ast, "col")
    txt.add("invalid 'return this' in \"$subName\"." +
      " only allowed in constructors (file: ${FIL.fileName} [$row/$col])")
    val showPos = showErrorPos(row.toInt(), col.toInt())
    showPos.forEach {
      txt.add(it)
    }
    return txt.joinToString("\n")
  }

  private fun showLine(ast: AstTree) {
    val row = astInfo(ast, "stmtLine").toInt()
    vmw.comment("#J %3d %s".format(row, FIL.lines[row-1]))
  }
}
