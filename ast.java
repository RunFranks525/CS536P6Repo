import java.io.*;
import java.util.*;

abstract class ASTnode {
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    /**
     * nameAnalysis
     * Creates an empty symbol table for the outermost scope, then processes
     * all of the globals, struct defintions, and functions in the program.
     */
    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab, 0);
        SemSym mainSymbol = symTab.lookupGlobal("main");
        if(mainSymbol == null){
          //Report error "No Main Function" 0,0
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck() {
        myDeclList.typeCheck();
    }


    public void codeGen(){
	     myDeclList.codeGen();
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    // 1 kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, process all of the decls in the list.
     */
    public void nameAnalysis(SymTable symTab, int initialOffset) {
        nameAnalysis(symTab, symTab, initialOffset);
    }

    public int totalOffsetSize(){      
      return myDecls.size() * 4;
    }

    public void codeGen(){
	     for (DeclNode node : myDecls) {
		       node.codeGen();
	      }
    }

//TODO: maybe need to delete
    public int length() {
	return myDecls.size();
    }


    public void nameAnalysis(SymTable symTab, SymTable globalTab, int initialOffset) {
        for (int i = 0; i < myDecls.size(); i++) {
	    DeclNode node = myDecls.get(i);
	    int offset = -(i * 4);
            if (node instanceof VarDeclNode) {
                ((VarDeclNode)node).nameAnalysis(symTab, globalTab, (offset + initialOffset));
            } else {
                node.nameAnalysis(symTab, 0);
            }
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck() {
        for (DeclNode node : myDecls) {
            node.typeCheck();
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * for each formal decl in the list
     *     process the formal decl
     *     if there was no error, add type of formal decl to list
     */
    public List<Type> nameAnalysis(SymTable symTab) {
        List<Type> typeList = new LinkedList<Type>();
        for (FormalDeclNode node : myFormals) {
            SemSym sym = node.nameAnalysis(symTab, 0);
            if (sym != null) {
                typeList.add(sym.getType());
            }
        }
        return typeList;
    }

    public int totalFormalsOffsetSize(){
      int totalFormalOffsetSize = 0;
      for(FormalDeclNode node : myFormals){
        totalFormalOffsetSize += node.getOffsetSize();
      }
      return totalFormalOffsetSize;
    }

    /**
     * Return the number of formals in this list.
     */
    public int length() {
        return myFormals.size();
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the declaration list
     * - process the statement list
     */
    public void nameAnalysis(SymTable symTab) {
        myDeclList.nameAnalysis(symTab, 0);
        myStmtList.nameAnalysis(symTab, totalLocalsOffsetSize());
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        myStmtList.typeCheck(retType);
    }

    public int totalLocalsOffsetSize(){
      return myDeclList.length() * 4;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public void codeGen(){	
	myStmtList.codeGen();
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, process each statement in the list.
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab, currentOffset);
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        for(StmtNode node : myStmts) {
            node.typeCheck(retType);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    public void codeGen() {
	for(StmtNode node : myStmts) {
	     node.codeGen();
	}
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public int size() {
        return myExps.size();
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, process each exp in the list.
     */
    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck(List<Type> typeList) {
        int k = 0;
        try {
            for (ExpNode node : myExps) {
                Type actualType = node.typeCheck();     // actual type of arg

                if (!actualType.isErrorType()) {        // if this is not an error
                    Type formalType = typeList.get(k);  // get the formal type
                    if (!formalType.equals(actualType)) {
                        ErrMsg.fatal(node.lineNum(), node.charNum(),
                                     "Type of actual does not match type of formal");
                    }
                }
                k++;
            }
        } catch (NoSuchElementException e) {
            System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
            System.exit(-1);
        }
    }

    public void codeGen() {
	for(ExpNode exp : myExps) {
		exp.codeGen();
	}
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    /**
     * Note: a formal decl needs to return a sym
     */
    abstract public SemSym nameAnalysis(SymTable symTab, int initialOffset);
    public void codeGen() { }

    public int getOffsetSize(){ return 0; }

    // default version of typeCheck for non-function decls
    public void typeCheck() { }
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    /**
     * nameAnalysis (overloaded)
     * Given a symbol table symTab, do:
     * if this name is declared void, then error
     * else if the declaration is of a struct type,
     *     lookup type name (globally)
     *     if type name doesn't exist, then error
     * if no errors so far,
     *     if name has already been declared in this scope, then error
     *     else add name to local symbol table
     *
     * symTab is local symbol table (say, for struct field decls)
     * globalTab is global symbol table (for struct type names)
     * symTab and globalTab can be the same
     */
    public SemSym nameAnalysis(SymTable symTab, int initialOffset) {
        return nameAnalysis(symTab, symTab, initialOffset);
    }

    public SemSym nameAnalysis(SymTable symTab, SymTable globalTab, int offset) {
        boolean badDecl = false;
        String name = myId.name();
        SemSym sym = null;
        IdNode structId = null;
        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Non-function declared void");
            badDecl = true;
        }

        else if (myType instanceof StructNode) {
            structId = ((StructNode)myType).idNode();
            sym = globalTab.lookupGlobal(structId.name());

            // if the name for the struct type is not found,
            // or is not a struct type
            if (sym == null || !(sym instanceof StructDefSym)) {
                ErrMsg.fatal(structId.lineNum(), structId.charNum(),
                             "Invalid name of struct type");
                badDecl = true;
            }
            else {
                structId.link(sym);
            }
        }

        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Multiply declared identifier");
            badDecl = true;
        }

        if (!badDecl) {  // insert into symbol table
            try {
                if (myType instanceof StructNode) {
                    sym = new StructSym(structId);
                }
                else {
                    sym = new SemSym(myType.type(), offset);
		    System.out.println(myId.name() + ": " + offset);
                }
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }

        return sym;
    }

    public void codeGen() {
	String offset = "4";
	Codegen.generate(".data");
	Codegen.generate(".align 2");
	Codegen.p.print("_" + myId.name() + ":");
	Codegen.p.println("\t.space " + offset);
	myId.sym().setIsGlobal(true);
    }

    public int getOffsetSize(){
      int symOffset = myId.sym().getSymOffsetSize();
      System.out.println(symOffset);
      return symOffset;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.println(";");
    }

    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name has already been declared in this scope, then error
     * else add name to local symbol table
     * in any case, do the following:
     *     enter new scope
     *     process the formals
     *     if this function is not multiply declared,
     *         update symbol table entry with types of formals
     *     process the body of the function
     *     exit scope
     */
    public SemSym nameAnalysis(SymTable symTab, int initialOffset) {
        String name = myId.name();
        FnSym sym = null;

        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Multiply declared identifier");
        }

        else { // add function name to local symbol table
            try {
                sym = new FnSym(myType.type(), myFormalsList.length());
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in FnDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in FnDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }

        symTab.addScope();  // add a new scope for locals and params

        // process the formals
        List<Type> typeList = myFormalsList.nameAnalysis(symTab);
	int totalFormalsOffsetSize = computeOffsetFromFormals();

	//build up offsets
	sym.setFormalsOffsetSize(totalFormalsOffsetSize);
	if (sym != null) {
            sym.addFormals(typeList);
        }
        myBody.nameAnalysis(symTab); // process the function body
	int totalLocalsOffsetSize = computeOffsetFromLocals();
        sym.setLocalsOffsetSize(totalLocalsOffsetSize);

	try {
            symTab.removeScope();  // exit scope
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        }

        return null;
    }

    private int computeOffsetFromFormals(){
      return myFormalsList.totalFormalsOffsetSize();
    }

    private int computeOffsetFromLocals(){
      return myBody.totalLocalsOffsetSize();
    }

    private void genFnPreamble() {
 	Codegen.p.println(".text");
	if(myId.name().equals("main")){
		Codegen.p.println(".globl main");
		Codegen.genLabel(myId.name(), "function decl for " + myId.name());
		Codegen.p.println("__start:");
	} else {
		Codegen.genLabel("_" + myId.name(), "function decl for " + myId.name());
	}

    }

    private void genFnPrologue(int totalParamsOffset, int totalLocalsOffset) {
	Codegen.genPush(Codegen.RA);
	Codegen.genPush(Codegen.FP);
	Codegen.generate("addu", Codegen.FP, Codegen.SP, totalParamsOffset + 8); //size of params + 8
    	Codegen.generate("subu", Codegen.SP, Codegen.SP, totalLocalsOffset);
    }

    private void genFnBody() {
	myBody.codeGen();
    }

    private void genFnEpilogue(int totalParamsOffset, int totalLocalsOffset) {
	 if(myId.name().equals("main")){
		Codegen.generate("li", Codegen.V0, 10);
		Codegen.generate("syscall");
	 } else {
		Codegen.generateIndexed("lw", Codegen.RA, Codegen.FP, totalParamsOffset);
	 	Codegen.generate("move", Codegen.T0, Codegen.FP);
	 	Codegen.generateIndexed("lw", Codegen.FP, Codegen.FP, (totalParamsOffset + 4));
	 	Codegen.generate("move", Codegen.SP, Codegen.T0);
	 	Codegen.generate("jr", Codegen.RA);
	 }	
 	 
    }

    public void codeGen(){
	int totalParamsOffset = computeOffsetFromFormals();
	int totalLocalsOffset = computeOffsetFromLocals();
	genFnPreamble();
	Codegen.p.println();
	genFnPrologue(totalParamsOffset, totalLocalsOffset);
	Codegen.p.println();
	genFnBody();
	Codegen.p.println();
	genFnEpilogue(totalParamsOffset, totalLocalsOffset);
	Codegen.p.println();
    }

    /**
     * typeCheck
     */
    public void typeCheck() {
        myBody.typeCheck(myType.type());
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this formal is declared void, then error
     * else if this formal is already in the local symble table,
     *     then issue multiply declared error message and return null
     * else add a new entry to the symbol table and return that Sym
     */
    public SemSym nameAnalysis(SymTable symTab, int initialOffset) {
        String name = myId.name();
        boolean badDecl = false;
        SemSym sym = null;
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Non-function declared void");
            badDecl = true;
        }

        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Multiply declared identifier");
            badDecl = true;
        }

        if (!badDecl) {  // insert into symbol table
            try {
                sym = new SemSym(myType.type(), 4);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }

        return sym;
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
    }

    public int getOffsetSize(){
      return myId.sym().getSymOffsetSize();
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name is already in the symbol table,
     *     then multiply declared error (don't add to symbol table)
     * create a new symbol table for this struct definition
     * process the decl list
     * if no errors
     *     add a new entry to symbol table for this struct
     */
    public SemSym nameAnalysis(SymTable symTab, int offset) {
        String name = myId.name();
        boolean badDecl = false;

        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Multiply declared identifier");
            badDecl = true;
        }

        SymTable structSymTab = new SymTable();

        // process the fields of the struct
        myDeclList.nameAnalysis(structSymTab, symTab, 0);

        if (!badDecl) {
            try {   // add entry to symbol table
                StructDefSym sym = new StructDefSym(structSymTab);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                                   " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }

        return null;
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        p.print(myId.name());
        p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }

    // 2 kids
    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    /* all subclasses must provide a type method */
    abstract public Type type();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    /**
     * type
     */
    public Type type() {
        return new IntType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    /**
     * type
     */
    public Type type() {
        return new BoolType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    /**
     * type
     */
    public Type type() {
        return new VoidType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public IdNode idNode() {
        return myId;
    }

    /**
     * type
     */
    public Type type() {
        return new StructType(myId);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        p.print(myId.name());
    }

    // 1 kid
    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalysis(SymTable symTab, int currentOffset);
    abstract public void typeCheck(Type retType);
    abstract public void codeGen();
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myAssign.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        myAssign.typeCheck();
    }

    public void codeGen(){
	myAssign.codeGen();
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    // 1 kid
    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator applied to non-numeric operand");
        }
    }


    public void codeGen(){
	//1. Eval the RHS expression, leaving the value on the stack
	myExp.codeGen(); //result is pushed onto top of stack
	//2. Push the address of the LHS ID onto the stack
	((IdNode)myExp).genAddr(); //Addr of LHS pushed onto stack
	//3. Store the value into the address
	Codegen.genPop(Codegen.T1); //place addr into T1 by popping from stack
	Codegen.genPop(Codegen.T0); //place value to store into T0
	Codegen.generate("addi", Codegen.T0, Codegen.T0, 1);
	Codegen.generateIndexed("sw", Codegen.T0, Codegen.T1, 0); //
	//4. Leave a copy of the value on the stack
	Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    // 1 kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator applied to non-numeric operand");
        }
    }


    public void codeGen(){
	//1. Eval the RHS expression, leaving the value on the stack
	myExp.codeGen(); //result is pushed onto top of stack
	//2. Push the address of the LHS ID onto the stack
	((IdNode)myExp).genAddr(); //Addr of LHS pushed onto stack
	//3. Store the value into the address
	Codegen.genPop(Codegen.T1); //place addr into T1 by popping from stack
	Codegen.genPop(Codegen.T0); //place value to store into T0
	Codegen.generate("subi", Codegen.T0, Codegen.T0, 1);
	Codegen.generateIndexed("sw", Codegen.T0, Codegen.T1, 0); //
	//4. Leave a copy of the value on the stack
	Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    // 1 kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to read a function");
        }

        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to read a struct name");
        }

        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to read a struct variable");
        }
    }


    public void codeGen(){
	Codegen.generate("li", Codegen.V0, 5); 
	Codegen.generate("syscall"); //says to do this in the notes
	myExp.genAddr(); //addr of Expr placed on top of stack
	Codegen.genPop(Codegen.T0);
	Codegen.generateIndexed("sw", Codegen.V0, Codegen.T0, 0); //Value from read should be in V0
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    private Type typeOfExp;

    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
        typeOfExp = null;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to write a function");
        }

        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to write a struct name");
        }

        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to write a struct variable");
        }

        if (type.isVoidType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Attempt to write void");
        }

        this.typeOfExp = type;
    }


    public void codeGen(){
	myExp.codeGen(); //result of Expr placed on top of stack
	Codegen.genPop(Codegen.A0); //pop TOS into register $a0
	if(this.typeOfExp.isStringType()) {
		Codegen.generate("li", Codegen.V0, 4); //set V0 to 4 for strings
	} else {
		Codegen.generate("li", Codegen.V0, 1); //set V0 to 1 for bools and ints
	}
	Codegen.generate("syscall"); //says to do this in the notes

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab, currentOffset);
        myStmtList.nameAnalysis(symTab, currentOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    }

     /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression used as an if condition");
        }

        myStmtList.typeCheck(retType);
    }


    public void codeGen(){
	String labelStr = Codegen.nextLabel();
	myExp.codeGen(); //result goes to T0
	Codegen.generate("li", Codegen.T1, 0);
	Codegen.generate("beq", Codegen.T0, Codegen.T1, labelStr);
        //myDeclList.codeGen();

	myStmtList.codeGen();
	Codegen.genLabel(labelStr);

    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts of then
     * - exit the scope
     * - enter a new scope
     * - process the decls and stmts of else
     * - exit the scope
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myThenDeclList.nameAnalysis(symTab, currentOffset);
        myThenStmtList.nameAnalysis(symTab, currentOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
        symTab.addScope();
        myElseDeclList.nameAnalysis(symTab, currentOffset);
        myElseStmtList.nameAnalysis(symTab, currentOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression used as an if condition");
        }

        myThenStmtList.typeCheck(retType);
        myElseStmtList.typeCheck(retType);
    }


    public void codeGen(){
	String labelStr1 = Codegen.nextLabel();
	String labelStr2 = Codegen.nextLabel();
	myExp.codeGen();
	Codegen.generate("li", Codegen.T1, 0);
	Codegen.generate("beq", Codegen.T0, Codegen.T1, labelStr1);
        myThenDeclList.codeGen();
	myThenStmtList.codeGen();
	Codegen.generate("j", labelStr2);
	Codegen.genLabel(labelStr1);
	myElseDeclList.codeGen();
	myElseStmtList.codeGen();
	Codegen.genLabel(labelStr2);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab, currentOffset);
        myStmtList.nameAnalysis(symTab, currentOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();

        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-bool expression used as a while condition");
        }

        myStmtList.typeCheck(retType);
    }

    public void codeGen(){
	String labelStr1 = Codegen.nextLabel();
	String labelStr2 = Codegen.nextLabel();
	Codegen.genLabel(labelStr1);
	myExp.codeGen(); // result is placed in T0
	Codegen.generate("li", Codegen.T1, 0);
	Codegen.generate("beq", Codegen.T0, Codegen.T1, labelStr2);
	myDeclList.codeGen();
	myStmtList.codeGen();
	Codegen.generate("j", labelStr1);
	Codegen.genLabel(labelStr2);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        myCall.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        myCall.typeCheck();
    }

    public void codeGen(){
	myCall.codeGen();
	Codegen.genPop(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    // 1 kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child,
     * if it has one
     */
    public void nameAnalysis(SymTable symTab, int currentOffset) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    }

    /**
     * typeCheck
     */
    public void typeCheck(Type retType) {
        if (myExp != null) {  // return value given
            Type type = myExp.typeCheck();

            if (retType.isVoidType()) {
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Return with a value in a void function");
            }

            else if (!retType.isErrorType() && !type.isErrorType() && !retType.equals(type)){
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Bad return value");
            }
        }

        else {  // no return value given -- ok if this is a void function
            if (!retType.isVoidType()) {
                ErrMsg.fatal(0, 0, "Missing return value");
            }
        }

    }


    public void codeGen(){
	myExp.codeGen(); //Places result into T0
	Codegen.genPop(Codegen.T0);
	Codegen.generate("addi", Codegen.V0, Codegen.T0, 0);
	//Codegen.generate("move", Codegen.V0, Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    /**
     * Default version for nodes with no names
     */
    public void nameAnalysis(SymTable symTab) { }

    abstract public Type typeCheck();
    abstract public int lineNum();
    abstract public int charNum();
    abstract public void codeGen();
    public void genAddr(){
	//do nothing
    }
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    /**
     * Return the line number for this literal.
     */
    public int lineNum() {
        return myLineNum;
    }

    /**
     * Return the char number for this literal.
     */
    public int charNum() {
        return myCharNum;
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        return new IntType();
    }


    public void codeGen(){
	Codegen.generate("li", Codegen.T0, myIntVal);
	Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    /**
     * Return the line number for this literal.
     */
    public int lineNum() {
        return myLineNum;
    }

    /**
     * Return the char number for this literal.
     */
    public int charNum() {
        return myCharNum;
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        return new StringType();
    }

    public void codeGen(){
	String label = Codegen.nextLabel();
	Codegen.generate(".data");
      	Codegen.p.print(label + ":");
	Codegen.p.println("\t.asciiz " + myStrVal);
	Codegen.generate(".text");
	Codegen.generate("la", Codegen.T0, label);
	Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    /**
     * Return the line number for this literal.
     */
    public int lineNum() {
        return myLineNum;
    }

    /**
     * Return the char number for this literal.
     */
    public int charNum() {
        return myCharNum;
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        return new BoolType();
    }

    public void codeGen(){
	    Codegen.generate("li", Codegen.T0, 1);
	    Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    /**
     * Return the line number for this literal.
     */
    public int lineNum() {
        return myLineNum;
    }

    /**
     * Return the char number for this literal.
     */
    public int charNum() {
        return myCharNum;
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        return new BoolType();
    }


    public void codeGen(){
	Codegen.generate("li", Codegen.T0, 0);
	Codegen.genPush(Codegen.T0);

    }
    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    /**
     * Link the given symbol to this ID.
     */
    public void link(SemSym sym) {
        mySym = sym;
    }

    /**
     * Return the name of this ID.
     */
    public String name() {
        return myStrVal;
    }

    /**
     * Return the symbol associated with this ID.
     */
    public SemSym sym() {
        return mySym;
    }

    /**
     * Return the line number for this ID.
     */
    public int lineNum() {
        return myLineNum;
    }

    /**
     * Return the char number for this ID.
     */
    public int charNum() {
        return myCharNum;
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - check for use of undeclared name
     * - if ok, link to symbol table entry
     */
    public void nameAnalysis(SymTable symTab) {
        SemSym sym = symTab.lookupGlobal(myStrVal);
        if (sym == null) {
            ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
        } else {
            link(sym);
        }
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        if (mySym != null) {
            return mySym.getType();
        }
        else {
            System.err.println("ID with null sym field in IdNode.typeCheck");
            System.exit(-1);
        }
        return null;
    }

    public void genJumpAndLink(){
	String jumpLabel = "_" + myStrVal;
	Codegen.generate("jal", jumpLabel);
    }

    public void codeGen(){
	if(mySym.isGlobal()){
		Codegen.generate("lw", Codegen.T0, "_"+ myStrVal);
	}
	else{
		int offset = mySym.getSymOffsetSize();
		Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, offset);
	}
	Codegen.genPush(Codegen.T0);
    }



    public void genAddr(){
	int offset = mySym.getSymOffsetSize();
	if(mySym.isGlobal()){
		Codegen.generate("la", Codegen.T0, "_"+ myStrVal);
	}
	else{
		Codegen.generateIndexed("la", Codegen.T0, Codegen.FP, offset);
	}
	Codegen.genPush(Codegen.T0);

    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (mySym != null) {
            p.print("(" + mySym + ")");
        }
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private SemSym mySym;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
        mySym = null;
    }

    /**
     * Return the symbol associated with this dot-access node.
     */
    public SemSym sym() {
        return mySym;
    }

    /**
     * Return the line number for this dot-access node.
     * The line number is the one corresponding to the RHS of the dot-access.
     */
    public int lineNum() {
        return myId.lineNum();
    }

    /**
     * Return the char number for this dot-access node.
     * The char number is the one corresponding to the RHS of the dot-access.
     */
    public int charNum() {
        return myId.charNum();
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the LHS of the dot-access
     * - process the RHS of the dot-access
     * - if the RHS is of a struct type, set the sym for this node so that
     *   a dot-access "higher up" in the AST can get access to the symbol
     *   table for the appropriate struct definition
     */
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable structSymTab = null; // to lookup RHS of dot-access
        SemSym sym = null;

        myLoc.nameAnalysis(symTab);  // do name analysis on LHS

        // if myLoc is really an ID, then sym will be a link to the ID's symbol
        if (myLoc instanceof IdNode) {
            IdNode id = (IdNode)myLoc;
            sym = id.sym();

            // check ID has been declared to be of a struct type

            if (sym == null) { // ID was undeclared
                badAccess = true;
            }
            else if (sym instanceof StructSym) {
                // get symbol table for struct type
                SemSym tempSym = ((StructSym)sym).getStructType().sym();
                structSymTab = ((StructDefSym)tempSym).getSymTable();
            }
            else {  // LHS is not a struct type
                ErrMsg.fatal(id.lineNum(), id.charNum(),
                             "Dot-access of non-struct type");
                badAccess = true;
            }
        }

        // if myLoc is really a dot-access (i.e., myLoc was of the form
        // LHSloc.RHSid), then sym will either be
        // null - indicating RHSid is not of a struct type, or
        // a link to the Sym for the struct type RHSid was declared to be
        else if (myLoc instanceof DotAccessExpNode) {
            DotAccessExpNode loc = (DotAccessExpNode)myLoc;

            if (loc.badAccess) {  // if errors in processing myLoc
                badAccess = true; // don't continue proccessing this dot-access
            }
            else { //  no errors in processing myLoc
                sym = loc.sym();

                if (sym == null) {  // no struct in which to look up RHS
                    ErrMsg.fatal(loc.lineNum(), loc.charNum(),
                                 "Dot-access of non-struct type");
                    badAccess = true;
                }
                else {  // get the struct's symbol table in which to lookup RHS
                    if (sym instanceof StructDefSym) {
                        structSymTab = ((StructDefSym)sym).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in DotAccessExpNode");
                        System.exit(-1);
                    }
                }
            }

        }

        else { // don't know what kind of thing myLoc is
            System.err.println("Unexpected node type in LHS of dot-access");
            System.exit(-1);
        }

        // do name analysis on RHS of dot-access in the struct's symbol table
        if (!badAccess) {

            sym = structSymTab.lookupGlobal(myId.name()); // lookup
            if (sym == null) { // not found - RHS is not a valid field name
                ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                             "Invalid struct field name");
                badAccess = true;
            }

            else {
                myId.link(sym);  // link the symbol
                // if RHS is itself as struct type, link the symbol for its struct
                // type to this dot-access node (to allow chained dot-access)
                if (sym instanceof StructSym) {
                    mySym = ((StructSym)sym).getStructType().sym();
                }
            }
        }
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        return myId.typeCheck();
    }

    public void codeGen(){

    }

    public void unparse(PrintWriter p, int indent) {
        myLoc.unparse(p, 0);
        p.print(".");
        myId.unparse(p, 0);
    }

    // 2 kids
    private ExpNode myLoc;
    private IdNode myId;
    private SemSym mySym;          // link to Sym for struct type
    private boolean badAccess;  // to prevent multiple, cascading errors
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    /**
     * Return the line number for this assignment node.
     * The line number is the one corresponding to the left operand.
     */
    public int lineNum() {
        return myLhs.lineNum();
    }

    /**
     * Return the char number for this assignment node.
     * The char number is the one corresponding to the left operand.
     */
    public int charNum() {
        return myLhs.charNum();
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's
     * two children
     */
    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type typeLhs = myLhs.typeCheck();
        Type typeExp = myExp.typeCheck();
        Type retType = typeLhs;

        if (typeLhs.isFnType() && typeExp.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Function assignment");
            retType = new ErrorType();
        }

        if (typeLhs.isStructDefType() && typeExp.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct name assignment");
            retType = new ErrorType();
        }

        if (typeLhs.isStructType() && typeExp.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct variable assignment");
            retType = new ErrorType();
        }

        if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Type mismatch");
            retType = new ErrorType();
        }

        if (typeLhs.isErrorType() || typeExp.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){
	int offset = ((IdNode)myLhs).sym().getSymOffsetSize();
	System.out.println(offset);
	//1. Eval the RHS expression, leaving the value on the stack
	myExp.codeGen(); //result is pushed onto top of stack
	//2. Push the address of the LHS ID onto the stack
	((IdNode)myLhs).genAddr(); //Addr of LHS pushed onto stack
	//3. Store the value into the address
	Codegen.genPop(Codegen.T1); //place addr into T0 by popping from stack
	Codegen.genPop(Codegen.T0); //place value to store into T1
	Codegen.generateIndexed("sw", Codegen.T0, Codegen.T1, offset); //
	//4. Leave a copy of the value on the stack
	Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");
    }

    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    /**
     * Return the line number for this call node.
     * The line number is the one corresponding to the function name.
     */
    public int lineNum() {
        return myId.lineNum();
    }

    /**
     * Return the char number for this call node.
     * The char number is the one corresponding to the function name.
     */
    public int charNum() {
        return myId.charNum();
    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's
     * two children
     */
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        if (!myId.typeCheck().isFnType()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Attempt to call a non-function");
            return new ErrorType();
        }

        FnSym fnSym = (FnSym)(myId.sym());

        if (fnSym == null) {
            System.err.println("null sym for Id in CallExpNode.typeCheck");
            System.exit(-1);
        }

        if (myExpList.size() != fnSym.getNumParams()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                         "Function call with wrong number of args");
            return fnSym.getReturnType();
        }

        myExpList.typeCheck(fnSym.getParamTypes());
        return fnSym.getReturnType();
    }

    public void codeGen(){
	//1. Evaluate each actual parameter, push the values onto the stack
	myExpList.codeGen();
	//2. Jump and link (Jump to the called function, leaving the return addr in the RA register)
	myId.genJumpAndLink();
	//3. Push the returned value (V0 or F0) onto the stack
	Codegen.genPush(Codegen.V0);
	
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    /**
     * Return the line number for this unary expression node.
     * The line number is the one corresponding to the  operand.
     */
    public int lineNum() {
        return myExp.lineNum();
    }

    /**
     * Return the char number for this unary expression node.
     * The char number is the one corresponding to the  operand.
     */
    public int charNum() {
        return myExp.charNum();
    }

    public void codeGen(){

    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     */
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    /**
     * Return the line number for this binary expression node.
     * The line number is the one corresponding to the left operand.
     */
    public int lineNum() {
        return myExp1.lineNum();
    }

    /**
     * Return the char number for this binary expression node.
     * The char number is the one corresponding to the left operand.
     */
    public int charNum() {
        return myExp1.charNum();
    }

    public void codeGen(){

    }

    /**
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's
     * two children
     */
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new IntType();

        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        }

        if (type.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){
	myExp.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("li", Codegen.T1, 0);
      	Codegen.generate("addi", Codegen.T1, Codegen.T1, -1);
        Codegen.generate("xor", Codegen.T0, Codegen.T0, Codegen.T1);
	//negate
	Codegen.generate("addi", Codegen.T0, Codegen.T0, 1); //add one
	Codegen.genPush(Codegen.T0); // push result to stack
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new BoolType();

        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        }

        if (type.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){
	    myExp.codeGen(); // will push result to stack
	    Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
            Codegen.generate("li", Codegen.T1, 0);
      	    Codegen.generate("addi", Codegen.T1, Codegen.T1, -1);
      	    Codegen.generate("xor", Codegen.T0, Codegen.T0, Codegen.T1);
	    Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

abstract class ArithmeticExpNode extends BinaryExpNode {
    public ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new IntType();

        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        }

        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        }

        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){

    }

}

abstract class LogicalExpNode extends BinaryExpNode {
    public LogicalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();

        if (!type1.isErrorType() && !type1.isBoolType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        }

        if (!type2.isErrorType() && !type2.isBoolType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        }

        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){

    }
}

abstract class EqualityExpNode extends BinaryExpNode {
    public EqualityExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();

        if (type1.isVoidType() && type2.isVoidType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator applied to void functions");
            retType = new ErrorType();
        }

        if (type1.isFnType() && type2.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator applied to functions");
            retType = new ErrorType();
        }

        if (type1.isStructDefType() && type2.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator applied to struct names");
            retType = new ErrorType();
        }

        if (type1.isStructType() && type2.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator applied to struct variables");
            retType = new ErrorType();
        }

        if (!type1.equals(type2) && !type1.isErrorType() && !type2.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Type mismatch");
            retType = new ErrorType();
        }

        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){

    }
}

abstract class RelationalExpNode extends BinaryExpNode {
    public RelationalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    /**
     * typeCheck
     */
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType();

        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Relational operator applied to non-numeric operand");
            retType = new ErrorType();
        }

        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Relational operator applied to non-numeric operand");
            retType = new ErrorType();
        }

        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }

        return retType;
    }

    public void codeGen(){

    }
}

class PlusNode extends ArithmeticExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("addu", Codegen.T0, Codegen.T0, Codegen.T1); //T0 has 1 if myExp1 >= myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class MinusNode extends ArithmeticExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("subu", Codegen.T0, Codegen.T0, Codegen.T1); //T0 has 1 if myExp1 >= myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class TimesNode extends ArithmeticExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }


    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generateWithComment("mult", "Multiplication Operation", Codegen.T1, Codegen.T0);
	Codegen.generateWithComment("mflo", "Move from lo to T1", Codegen.T1);
	Codegen.genPush(Codegen.T1); // push result to stack
    }
}

class DivideNode extends ArithmeticExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("div", "Division Operation", Codegen.T1, Codegen.T0);
	Codegen.generate("mflo", "Move from lo to T1", Codegen.T1);
	Codegen.genPush(Codegen.T1); // push result to stack
    }
}

class AndNode extends LogicalExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	//generate jump label
	String labelString1 = Codegen.nextLabel();
	Codegen.generate("li", Codegen.T1, "1", "Load truthy number into register T1");
	myExp1.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("bne", Codegen.T0, Codegen.T1, labelString1); //if myExp1 != True, jump to label
	//else, evaluate right operand, that value is result
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T0); //pop myExp2 result into T0
	Codegen.generate("bne", Codegen.T0, Codegen.T1, labelString1); //if myExp2 != True, jump to label
	//else, value of the whole expression is true
	Codegen.genPush(Codegen.T1); //push true to stack
	//jump here
	Codegen.genLabel(labelString1);
	Codegen.generate("li", Codegen.T1, "0", "Load falsey number into register T1");
	Codegen.genPush(Codegen.T1); //push false to stack
    }
}

class OrNode extends LogicalExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	//generate jump label
	String labelString1 = Codegen.nextLabel();
	Codegen.generate("li", Codegen.T1, "1", "Load truthy number into register T1");
	myExp1.codeGen(); //push result of myExp1 to stack
	Codegen.genPop(Codegen.T0); //pop myExp1 result into T0
	Codegen.generate("beq", Codegen.T0, Codegen.T1, labelString1); //if myExp1 == True, jump to label
	//else evaluate the right operand, the value is result
	myExp2.codeGen();
	Codegen.genPop(Codegen.T0); //pop result into T0
	Codegen.generate("beq", Codegen.T0, Codegen.T1, labelString1); //if myExp2 == True, jump to label
	//else, exp is false
	Codegen.generate("li", Codegen.T1, "0", "Load falsey number into register T1");
	Codegen.genPush(Codegen.T0);
	//else, lhs is true, no need to evaluate rest of expression, push lhs on stack
	Codegen.genLabel(labelString1);
	Codegen.genPush(Codegen.T0); //push true to stack
    }
}

class EqualsNode extends EqualityExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	String labelStr = Codegen.nextLabel();
	String labelStr1;
	String labelStr2;
	String labelStr3;
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0

	//Is this string lit stuff okay??
	if(myExp1 instanceof StringLitNode){
		labelStr1 = Codegen.nextLabel();
		labelStr2 = Codegen.nextLabel();
		labelStr3 = Codegen.nextLabel();
		Codegen.genLabel(labelStr1); //label for beginning of loop
		Codegen.generate("lb", Codegen.V0, Codegen.T0, 0); //get next char of 1st string
		Codegen.generate("lb", Codegen.V1, Codegen.T1, 0); //get next char of 2nd string
		Codegen.generate("bne", Codegen.V0, Codegen.V1, labelStr2); //compare the two strings
		Codegen.generate("li", Codegen.V1, 0); //load 0 into V1 to represent null char
		Codegen.generate("beq", Codegen.V0, Codegen.V1, labelStr3); //if end of string, they're equal!
		Codegen.generate("addi", Codegen.T0, Codegen.T0, 1); //increment addr1 by 1
		Codegen.generate("addi", Codegen.T1, Codegen.T1, 1); //increment addr2 by 1
		Codegen.generate("j", labelStr1); //jump to loop for next char

		//If Not Equals
		Codegen.genLabel(labelStr2);
		Codegen.generate("li", Codegen.T0, 0); //load 0 for not equals
		Codegen.generate("j", labelStr);
		//If Equals
		Codegen.genLabel(labelStr3);
		Codegen.generate("li", Codegen.T0, 1); //load 1 for equals
		Codegen.generate("j", labelStr);
	}
	else{
		//if comparing ints or bools
		Codegen.generate("li", Codegen.T0, 1); //Load 1 by default
		Codegen.generate("beq", Codegen.T0, Codegen.T1, labelStr); //push if equal
		Codegen.generate("li", Codegen.T0, 0); //otherwise load 0, then go to push
	}
	//code for any type, push to stack
	Codegen.genLabel(labelStr);
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class NotEqualsNode extends EqualityExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	String labelStr = Codegen.nextLabel(); //this is needed regardless
	//these are needed if comparing strings
	String labelStr1;
	String labelStr2;
	String labelStr3;
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0

	//Is this string lit stuff okay??
	if(myExp1 instanceof StringLitNode){
		//generate labels
		labelStr1 = Codegen.nextLabel();
		labelStr2 = Codegen.nextLabel();
		labelStr3 = Codegen.nextLabel();
		Codegen.genLabel(labelStr1); //label for beginning of loop
		Codegen.generate("lb", Codegen.V0, Codegen.T0, 0); //get next char of 1st string
		Codegen.generate("lb", Codegen.V1, Codegen.T1, 0); //get next char of 2nd string
		Codegen.generate("bne", Codegen.V0, Codegen.V1, labelStr2); //compare the two strings
		Codegen.generate("li", Codegen.V1, 0); //load 0 into V1 to represent null char
		Codegen.generate("beq", Codegen.V0, Codegen.V1, labelStr3); //if end of string, they're equal!
		Codegen.generate("addi", Codegen.T0, Codegen.T0, 1); //increment addr1 by 1
		Codegen.generate("addi", Codegen.T1, Codegen.T1, 1); //increment addr2 by 1
		Codegen.generate("j", labelStr1); //jump to loop for next char

		//If Not Equals
		Codegen.genLabel(labelStr2);
		Codegen.generate("li", Codegen.T0, 1); //load 1 for not equals
		Codegen.generate("j", labelStr);
		//If Equals
		Codegen.genLabel(labelStr3);
		Codegen.generate("li", Codegen.T0, 0); //load 0 for equals
		Codegen.generate("j", labelStr);
	}
	else{
		//if literal, not string
		Codegen.generate("li", Codegen.T0, 1); //Load 1 by default
		Codegen.generate("bne", Codegen.T0, Codegen.T1, labelStr); //push if not equal
		Codegen.generate("li", Codegen.T0, 0); //otherwise load 0, then go to push
	}
	//code for any type, push to stack
	Codegen.genLabel(labelStr);
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class LessNode extends RelationalExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("slt", Codegen.T0, Codegen.T0, Codegen.T1); //T0 has 1 if myExp1 < myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class GreaterNode extends RelationalExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("addu", Codegen.T0, Codegen.T0, 1);
	Codegen.generate("slt", Codegen.T0, Codegen.T1, Codegen.T0); //T0 has 1 if myExp1 > myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class LessEqNode extends RelationalExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("addu", Codegen.T1, Codegen.T1, 1);
	Codegen.generate("slt", Codegen.T0, Codegen.T0, Codegen.T1); //T0 has 1 if myExp1 <= myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}

class GreaterEqNode extends RelationalExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen(){
	myExp1.codeGen(); // will push result to stack
	myExp2.codeGen(); // will push result to stack
	Codegen.genPop(Codegen.T1); // Pop myExp2 result into T1
	Codegen.genPop(Codegen.T0); // pop myExp1 result into T0
	Codegen.generate("slt", Codegen.T0, Codegen.T1, Codegen.T0); //T0 has 1 if myExp1 >= myExp2
	Codegen.genPush(Codegen.T0); // push result to stack
    }
}
