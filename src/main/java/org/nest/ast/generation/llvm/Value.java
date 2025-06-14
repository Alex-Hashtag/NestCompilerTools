package org.nest.ast.generation.llvm;


import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import org.nest.ast.generation.llvm.types.Type;
import org.nest.ast.generation.llvm.types.TypeFactory;


public class Value {
    private final LLVMValueRef ref;

    public Value(LLVMValueRef ref) {
        this.ref = ref;
    }

    public LLVMValueRef getRef() {
        return ref;
    }

    public String getName() {
        return LLVM.LLVMGetValueName(ref).getString();
    }

    public void setName(String name) {
        LLVM.LLVMSetValueName(ref, name);
    }

    public Type getType() {
        LLVMTypeRef typeRef = LLVM.LLVMTypeOf(ref);
        return TypeFactory.fromLLVM(typeRef);
    }

    public boolean isConstant() {
        return LLVM.LLVMIsConstant(ref) != 0;
    }

    public boolean isNull() {
        return LLVM.LLVMIsNull(ref) != 0;
    }

    public boolean isUndef() {
        return LLVM.LLVMIsUndef(ref) != 0;
    }

    public void printToStderr() {
        LLVM.LLVMDumpValue(ref);
    }

    public boolean equals(Value other) {
        return LLVM.LLVMValueAsMetadata(ref).equals(LLVM.LLVMValueAsMetadata(other.ref));
    }

    public static Value from(LLVMValueRef ref) {
        return new Value(ref);
    }
}
