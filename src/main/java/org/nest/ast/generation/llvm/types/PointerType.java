package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;


public class PointerType extends Type
{
    public PointerType(LLVMTypeRef ref)
    {
        super(ref);
    }

    @Override
    public TypeKind getKind()
    {
        return TypeKind.POINTER;
    }
}
