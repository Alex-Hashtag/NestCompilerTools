package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;


public class FunctionType extends Type
{
    public FunctionType(LLVMTypeRef ref)
    {
        super(ref);
    }

    @Override
    public TypeKind getKind()
    {
        return TypeKind.FUNCTION;
    }
}
