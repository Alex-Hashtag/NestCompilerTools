package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.nest.ast.generation.llvm.Value;

public abstract class Type {
    protected final LLVMTypeRef ref;

    protected Type(LLVMTypeRef ref) {
        this.ref = ref;
    }

    public LLVMTypeRef getRef() {
        return ref;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + ref + ")";
    }

    public enum TypeKind {
        INT, FLOAT, POINTER, ARRAY, FUNCTION, STRUCT
    }

    public abstract TypeKind getKind();
    
    /**
     * Create a value of this type.
     * This is a default implementation that throws UnsupportedOperationException.
     * Subclasses should override this method if they support creating values.
     * 
     * @param value The value to create (interpretation depends on type)
     * @return A Value instance representing this value
     */
    public Value createValue(Object value) {
        throw new UnsupportedOperationException("Cannot create a value of type " + getKind());
    }
}
