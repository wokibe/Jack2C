// parser for the Jack2 compiler
// generates an abstract syntax tree (AST) from the tokens
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

import java.io.File

enum class ASTTYP {Class, ClassVarConst, ClassVarDecl, ConstDecl,
  Subroutines, SubroutineDecl, ParameterList, ParameterDecl,
  SubroutineBody, VarConstDecl, VarDecl, Statements, ArrayIndex,
  IfStatement, Condition, IfWhileBlock, ElseBlock, Constant,
  WhileStatement, BreakStatement, SubroutineCall, ReturnStatement,
  LetStatement, DoStatement, ExpressionList, Expression, Term}

typealias InfoType = HashMap<String, String>

// ==================================================================
class AstTree(val typ: ASTTYP) {

  companion object {
    var size = 1
  }
  var info: InfoType = hashMapOf()
  val leafs: ArrayList<AstTree> = arrayListOf()

  // save same info for the Generator
  fun addInfo(key: String, value: String) {
    info[key] = value
  }

  // save dependend ast's
  fun addAst(node: AstTree) {
    // dont add empty ASTs
    if ((node.info.size > 0) or (node.leafs.size > 0)) {
      leafs.add(node)
      AstTree.size++
    }
  }

  // extract types for the toString() method
  fun leafTypes(): ArrayList<ASTTYP> {
    val arr: ArrayList<ASTTYP> = arrayListOf()
    leafs.forEach {
      arr.add(it.typ)
    }
    return arr
  }

  override fun toString() = "($typ, $info, ${leafTypes()}) "
}

// ==================================================================
class Parser(val tokens: ArrayList<Token>) {

  // start with the root of the AST
  fun parse(): AstTree {
    val ts = TokenStream(tokens)
    val at = AstTree(ASTTYP.Class)
    ts.eatStr("class")
    at.addInfo("name", ts.eatTyp(TOKTYP.IDENTIFIER))
    ts.eatStr("{")
    at.addAst(doClassVarConst(ts))
    at.addAst(doSubroutines(ts))
    ts.eatStr("}")
    return at
  }

  // generate the ast/tree.ast file
  fun astDump(ast: AstTree) {
    val path = FIL.basePath + "/ast"
    checkDir(path)
    val astFile = path + "/" + FIL.baseName + ".ast"
    LOG.DEBUG("astFile: $astFile")
    val astf = File(astFile)
    val txt: ArrayList<String> = arrayListOf()

      fun dumpAst(at: AstTree, offset: String = "") {
        txt.add("${offset}$at")
        at.leafs.forEach {
          dumpAst(it, " " + offset)
        }
      }

    dumpAst(ast)
    astf.writeText(txt.joinToString("\n"))
  }

  // ==================================================================

  private fun doClassVarConst(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ClassVarConst)
    while (ts.tstStrs(arrayOf("static", "field", "const"))) {
      if (ts.tstStr("const"))
        at.addAst(doConstDecl(ts, "class"))
      else
        at.addAst(doClassVarDecl(ts))
    }
    LOG.DEBUG("doClassVarConstDecl: $at")
    return at
  }

  private fun doConstDecl(ts: TokenStream, kind: String): AstTree {
    val at = AstTree(ASTTYP.ConstDecl)
    ts.eatStr("const")
    at.addInfo("id", ts.eatTyp(TOKTYP.IDENTIFIER))
    at.addInfo("kind", kind)
    ts.eatStr("=")
    if (ts.tstTyp(TOKTYP.CHARCONSTANT)) {
      val char = ts.eatTyp(TOKTYP.CHARCONSTANT)
      at.addInfo("value", "${char[0].toInt()}")
    }
    else {
      var txt = ""
      if (ts.tstStr("-")) {
        ts.eatStr("-")
        txt = txt + "-"
      }
      txt = txt + ts.eatTyp(TOKTYP.INTEGER)
      at.addInfo("value", txt)
      ts.eatStr(";")
    }
    LOG.DEBUG("doConstDecl: $at")
    return at
  }

  private fun doClassVarDecl(ts: TokenStream): AstTree {
    val types = arrayOf("int", "char", "boolean")
    val at = AstTree(ASTTYP.ClassVarDecl)
    val kind = ts.eatStrs(arrayOf("static", "field"))
    at.addInfo("kind", kind)
    at.addInfo("typ", idOrTypes(types, ts))
    at.addInfo("ids", idList(ts))
    ts.eatStr(";")
    LOG.DEBUG("doClassVarDecl: $at")
    return at
  }

  private fun doSubroutines(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.Subroutines)
    while (ts.tstStrs(arrayOf("constructor", "function", "method")))
      at.addAst(doSubroutine(ts))
    LOG.DEBUG("doSubroutines: $at")
    return at
  }

  private fun doSubroutine(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.SubroutineDecl)
    val kinds = arrayOf("function", "constructor", "method")
    val types = arrayOf("int", "char", "boolean", "void")
    at.addInfo("kind", ts.eatStrs(kinds))
    at.addInfo("typ", idOrTypes(types, ts))
    at.addInfo("name", ts.eatTyp(TOKTYP.IDENTIFIER))
    ts.eatStr("(")
    at.addAst(doParameterList(ts))
    ts.eatStr(")")
    at.addAst(doSubroutineBody(ts))
    LOG.DEBUG("doSubroutine: $at")
    return at
  }

  private fun doParameterList(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ParameterList)
    if (!ts.tstStr(")")) {
      at.addAst(doParameterDecl(ts))
      while (ts.tstStr(",")) {
        ts.eatStr(",")
        at.addAst(doParameterDecl(ts))
      }
    }
    LOG.DEBUG("doParameterList: $at")
    return at
  }

  private fun doParameterDecl(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ParameterDecl)
    val types = arrayOf("int", "char", "boolean")
    at.addInfo("typ", idOrTypes(types, ts))
    at.addInfo("name", ts.eatTyp(TOKTYP.IDENTIFIER))
    LOG.DEBUG("doParameterDecl: $at")
    return at
  }

  private fun doSubroutineBody(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.SubroutineBody)
    ts.eatStr("{")
    at.addAst(doVarConstDecl(ts))
    at.addAst(doStatements(ts))
    ts.eatStr("}")
    LOG.DEBUG("doSubroutineBody: $at")
    return at
  }

  private fun doVarConstDecl(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.VarConstDecl)
    at.addInfo("force", "localTables")
    while (ts.tstStrs(arrayOf("var", "const"))) {
      if (ts.tstStr("const"))
        at.addAst(doConstDecl(ts, "const"))
      else
        at.addAst(doVarDecl(ts))
    }
    LOG.DEBUG("doVarConstDecl: $at")
    return at
  }

  private fun doVarDecl(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.VarDecl)
    val types = arrayOf("int", "char", "boolean")
    ts.eatStr("var")
    at.addInfo("typ", idOrTypes(types, ts))
    at.addInfo("ids", idList(ts))
    ts.eatStr(";")
    LOG.DEBUG("doVarDecl: $at")
    return at
  }

  private fun doStatements(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.Statements)
    val starters = arrayOf("let", "if", "while", "break", "do", "return")
    while (ts.tstStrs(starters)) {
      when (ts.peek().str) {
        "let"     -> at.addAst(doLetStatement(ts))
        "if"      -> at.addAst(doIfStatement(ts))
        "while"   -> at.addAst(doWhileStatement(ts))
        "break"   -> at.addAst(doBreakStatement(ts))
        "do"      -> at.addAst(doDoStatement(ts))
        "return"  -> at.addAst(doReturnStatement(ts))
      }
    LOG.DEBUG("DoStatements: $at")
    }
    return at
  }

  private fun doLetStatement(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.LetStatement)
    ts.eatStr("let")
    stmtLine(ts, at)
    at.addInfo("id", ts.eatTyp(TOKTYP.IDENTIFIER))
    // keep position for possible semantic error: unknown id
    savePos(ts, at)
    if (ts.tstStr("["))
      at.addAst(doArrayIndex(ts))
    ts.eatStr("=")
    at.addAst(doExpression(ts))
    ts.eatStr(";")
    LOG.DEBUG("doLetStatement: $at")
    return at
  }

  private fun doIfStatement(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.IfStatement)
    ts.eatStr("if")
    stmtLine(ts, at)
    at.addAst(doCondition(ts))
    at.addAst(doIfWhileBlock(ts))
    if (ts.tstStr("else"))
      at.addAst(doElseBlock(ts))
    LOG.DEBUG("doIfStatement: $at")
    return at
  }

  private fun doWhileStatement(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.WhileStatement)
    ts.eatStr("while")
    stmtLine(ts, at)
    at.addAst(doCondition(ts))
    at.addAst(doIfWhileBlock(ts))
    LOG.DEBUG("doWhileStatement: $at")
    return at
  }

  private fun doBreakStatement(ts: TokenStream): AstTree {
    LOG.DEBUG("doBreakStatement: $ts")
    val at = AstTree(ASTTYP.BreakStatement)
    ts.eatStr("break")
    stmtLine(ts, at)
    // keep position for possible semantic error: invalid break
    savePos(ts, at)
    ts.eatStr(";")
    LOG.DEBUG("doBreakStatement: $at")
    return at
  }

  private fun doDoStatement(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.DoStatement)
    ts.eatStr("do")
    stmtLine(ts, at)
    at.addAst(doSubroutineCall(ts))
    ts.eatStr(";")
    LOG.DEBUG("doDoStatement: $at")
    return at
  }

  private fun doReturnStatement(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ReturnStatement)
    ts.eatStr("return")
    stmtLine(ts, at)
    // keep position for possible semantic error: invalid/missing ret atribute
    savePos(ts, at)
    if (ts.tstStr(";"))
      at.addInfo("case", "void")
    else {
      at.addInfo("case", "expr")
      at.addAst(doExpression(ts))
      }
    ts.eatStr(";")
    LOG.DEBUG("doReturnStatement: $at")
    return at
  }

  private fun doExpression(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.Expression)
    val ops = arrayOf("+", "-", "*", "/", "&", "|", "<", ">", "=")
    var opQueue = ""
    LOG.DEBUG("doExpression/initial token: ${ts.peek()}")
    at.addAst(doTerm(ts))
    while (ts.tstStrs(ops)) {
      LOG.DEBUG("doTerm/while op: ${ts.peek()}")
      opQueue = opQueue + ts.eatStrs(ops)
      at.addAst(doTerm(ts))
    }
    at.addInfo("ops", opQueue)
    LOG.DEBUG("doExpression: $at")
    return at
  }

  private fun doTerm(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.Term)
    val unaryOps = arrayOf("-", "~")
    val consts = arrayOf(TOKTYP.INTEGER, TOKTYP.STRING,
       TOKTYP.KEYWORDCONSTANT,TOKTYP.CHARCONSTANT)
    LOG.DEBUG("doTerm/token: ${ts.peek()}")
    when {
      ts.tstTyps(consts)    -> {
        at.addInfo("case", "const")
        at.addAst(doConstant(ts, consts))
      }
      ts.tstStr("(")        -> {
        at.addInfo("case", "expr")
        ts.eatStr("(")
        at.addAst(doExpression(ts))
        ts.eatStr(")")
      }
      ts.tstStrs(unaryOps)  -> {
        at.addInfo("case", "unary")
        at.addInfo("unaryOp", ts.eatStrs(unaryOps))
        at.addAst(doTerm(ts))
      }
      ts.tstTyp(TOKTYP.IDENTIFIER) -> {
        if (ts.tstStrs(arrayOf("(", "."), 1)) {// lookahead 1
          at.addInfo("case", "subr")
          at.addAst(doSubroutineCall(ts))
        }
        else {
          at.addInfo("id", ts.eatTyp(TOKTYP.IDENTIFIER))
          savePos(ts, at)
          if (ts.tstStr("[")) {
            at.addInfo("case", "array")
            at.addAst(doArrayIndex(ts))
          }
          else
            at.addInfo("case", "id")
        }
      }
      else -> LOG.ERROR("doTerm: unexpected token ${ts.peek()}")
    }
    LOG.DEBUG("doTerm: $at")
    return at
  }

  private fun doConstant(ts: TokenStream, types: Array<TOKTYP>): AstTree {
    val at = AstTree(ASTTYP.Constant)
    at.addInfo("typ", "${ts.peek().typ}")
    at.addInfo("const", ts.eatTyps(types))
    LOG.DEBUG("doConstant: $at")
    return at
  }

  // ==================================================================
  private fun idOrTypes(tokens: Array<String>, ts: TokenStream) =
    if (ts.tstTyp(TOKTYP.IDENTIFIER))
      ts.eatTyp(TOKTYP.IDENTIFIER)
    else
      ts.eatStrs(tokens)

  private fun idList(ts: TokenStream): String {
    var ids: ArrayList<String> = arrayListOf()
    ids.add(ts.eatTyp(TOKTYP.IDENTIFIER))
    while (ts.tstStr(",")) {
      ts.eatStr(",")
      ids.add(ts.eatTyp(TOKTYP.IDENTIFIER))
    }
    return ids.joinToString(",")
  }

  private fun doArrayIndex(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ArrayIndex)
    ts.eatStr("[")
    at.addAst(doExpression(ts))
    ts.eatStr("]")
    LOG.DEBUG("doArrayIndex: $at")
    return at
  }

  private fun savePos(ts: TokenStream, at: AstTree) {
    val (row, col) = ts.lastPos()
    at.addInfo("row", "$row")
    at.addInfo("col", "$col")
  }

  private fun stmtLine(ts: TokenStream, at: AstTree) {
    val (row, _) = ts.lastPos()
    at.addInfo("stmtLine", "$row")
  }

  private fun doCondition(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.Condition)
    ts.eatStr("(")
    at.addAst(doExpression(ts))
    ts.eatStr(")")
    LOG.DEBUG("doCondition: $at")
    return at
  }

  private fun doIfWhileBlock(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.IfWhileBlock)
    ts.eatStr("{")
    at.addAst(doStatements(ts))
    ts.eatStr("}")
    LOG.DEBUG("doIfWhileBlock: $at")
    return at
  }

  private fun doElseBlock(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ElseBlock)
    ts.eatStr("else")
    ts.eatStr("{")
    at.addAst(doStatements(ts))
    ts.eatStr("}")
    LOG.DEBUG("doElseBlock: $at")
    return at
  }

  private fun doSubroutineCall(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.SubroutineCall)
    at.addInfo("id", ts.eatTyp(TOKTYP.IDENTIFIER))
    // keep position for possible semantic error: unknown method/function
    savePos(ts, at)
    if (ts.tstStr(".")) {
      ts.eatStr(".")
      at.addInfo("name", ts.eatTyp(TOKTYP.IDENTIFIER))
    }
    at.addAst(doExpressionList(ts))
    LOG.DEBUG("doSubroutineCall: $at")
    return at
  }

  private fun doExpressionList(ts: TokenStream): AstTree {
    val at = AstTree(ASTTYP.ExpressionList)
    ts.eatStr("(")
    if (!ts.tstStr(")")) {
      at.addAst(doExpression(ts))
      while (ts.tstStr(",")) {
        ts.eatStr(",")
        at.addAst(doExpression(ts))
      }
    }
    ts.eatStr(")")
    at.addInfo("size", at.leafs.size.toString())
    LOG.DEBUG("doExpressionList: $at")
    return at
  }
}
