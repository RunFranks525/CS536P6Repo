import java.util.*;

/**
 * The Sym class defines a symbol-table entry.
 * Each Sym contains a type (a Type).
 */
public class SemSym {
    private Type type;
    private int offset;
    private boolean isGlobal;

    public SemSym(Type type, int offset) {
        this.type = type;
	this.offset = offset;
	this.isGlobal = false;
    }

    public void setIsGlobal(boolean isGlobal) {
	this.isGlobal = isGlobal;
    }

    public boolean isGlobal() {
	return this.isGlobal;
    }

    public Type getType() {
        return type;
    }

    public int getSymOffsetSize(){
      return offset;
    }

    public String toString() {
        return type.toString();
    }

    public Type getReturnType(){
	     return null;
    }
}

/**
 * The FnSym class is a subclass of the Sym class just for functions.
 * The returnType field holds the return type and there are fields to hold
 * information about the parameters.
 */
class FnSym extends SemSym {
    // new fields
    private Type returnType;
    private int numParams;
    private List<Type> paramTypes;
    private int formalsOffsetSize;
    private int localsOffsetSize;

    public FnSym(Type type, int numparams) {
        super(new FnType(), 0);
        returnType = type;
        numParams = numparams;
    }

    public void addFormals(List<Type> L) {
        paramTypes = L;
    }

    public void setFormalsOffsetSize(int value) {
	formalsOffsetSize = value;
    }

    public int getFormalsOffsetSize() {
	return this.formalsOffsetSize;
    }

    public void setLocalsOffsetSize(int value) {
	localsOffsetSize = value;
    }

    public Type getReturnType() {
        return returnType;
    }

    public int getNumParams() {
        return numParams;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public String toString() {
        // make list of formals
        String str = "";
        boolean notfirst = false;
        for (Type type : paramTypes) {
            if (notfirst)
                str += ",";
            else
                notfirst = true;
            str += type.toString();
        }

        str += "->" + returnType.toString();
        return str;
    }
}

/**
 * The StructSym class is a subclass of the Sym class just for variables
 * declared to be a struct type.
 * Each StructSym contains a symbol table to hold information about its
 * fields.
 */
class StructSym extends SemSym {
    // new fields
    private IdNode structType;  // name of the struct type

    public StructSym(IdNode id) {
        super(new StructType(id), 0);
        structType = id;
    }

    public IdNode getStructType() {
        return structType;
    }
}

/**
 * The StructDefSym class is a subclass of the Sym class just for the
 * definition of a struct type.
 * Each StructDefSym contains a symbol table to hold information about its
 * fields.
 */
class StructDefSym extends SemSym {
    // new fields
    private SymTable symTab;

    public StructDefSym(SymTable table) {
        super(new StructDefType(), 0);
        symTab = table;
    }

    public SymTable getSymTable() {
        return symTab;
    }
}
