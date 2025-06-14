package org.nest.ast.generation.llvm;


import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import org.nest.ast.generation.llvm.types.Type;


public class Function {
    private final LLVMValueRef ref;
    private final LLVMContextRef ctx;

    Function(LLVMContextRef ctx,
             LLVMModuleRef module,
             String name,
             Type returnType,
             Type[] params,
             boolean variadic)
    {
        this.ctx = ctx;
        PointerPointer<LLVMTypeRef> paramRefs = new PointerPointer<>(params.length);
        for (int i = 0; i < params.length; i++) paramRefs.put(i, params[i].getRef());

        LLVMTypeRef fnType =
                LLVM.LLVMFunctionType(returnType.getRef(),
                        paramRefs, params.length,
                        variadic ? 1 : 0);
        ref = LLVM.LLVMAddFunction(module, name, fnType);

        // always create an entry block for convenience
        LLVM.LLVMAppendBasicBlockInContext(ctx, ref, "entry");
    }

    /*──────── Query & helpers ───*/
    public Value getArgument(int index) {
        return new Value(LLVM.LLVMGetParam(ref, index));
    }

    public BasicBlock getEntryBlock() {
        return new BasicBlock(LLVM.LLVMGetEntryBasicBlock(ref));
    }

    public BasicBlock createBlock(String name) {
        return new BasicBlock(
                LLVM.LLVMAppendBasicBlockInContext(ctx, ref, name)
        );
    }

    public IRBuilder getEntryBuilder() {
        IRBuilder b = new IRBuilder(ctx);
        b.moveToBlock(getEntryBlock());
        return b;
    }

    public LLVMValueRef getRef() { return ref; }
}