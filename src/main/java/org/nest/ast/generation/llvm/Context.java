package org.nest.ast.generation.llvm;

import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.global.LLVM;
import org.nest.ast.generation.LLVMPipelineComponent;
import org.nest.ast.generation.llvm.types.Type;
import org.nest.ast.generation.llvm.types.TypeFactory;


public class Context implements LLVMPipelineComponent
{
    private final LLVMContextRef context;
    private final LLVMModuleRef module;
    private final TypeFactory typeFactory;

    private Context(LLVMContextRef context)
    {
        LLVM.LLVMInitializeNativeTarget();
        LLVM.LLVMInitializeNativeAsmPrinter();
        this.context = context;
        this.module = LLVM.LLVMModuleCreateWithNameInContext("module", context);
        this.typeFactory = new TypeFactory(context);
    }

    public static Context create()
    {
        return new Context(LLVM.LLVMContextCreate());
    }


    public LLVMContextRef getContextRef()
    {
        return context;
    }

    public LLVMModuleRef getModuleRef()
    {
        return module;
    }

    public TypeFactory types()
    {
        return typeFactory;
    }

    public Function defineFunction(String name,
                                   Type returnType,
                                   Type[] paramTypes,
                                   boolean variadic)
    {
        return new Function(context, module, name, returnType, paramTypes, variadic);
    }

    public void dispose()
    {
        LLVM.LLVMContextDispose(context);
//        LLVM.LLVMDisposeModule(module);
    }

}
