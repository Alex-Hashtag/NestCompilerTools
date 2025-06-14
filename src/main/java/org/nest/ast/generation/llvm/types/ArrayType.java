package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;


public class ArrayType extends Type
{
    public ArrayType(LLVMTypeRef ref)
    {
        super(ref);
    }

    @Override
    public TypeKind getKind()
    {
        return TypeKind.ARRAY;
    }
}
