package org.nest.ast.generation.llvm;

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;


public class BasicBlock
{
    private final LLVMBasicBlockRef ref;

    BasicBlock(LLVMBasicBlockRef ref)
    {
        this.ref = ref;
    }

    LLVMBasicBlockRef getRef()
    {
        return ref;
    }
}