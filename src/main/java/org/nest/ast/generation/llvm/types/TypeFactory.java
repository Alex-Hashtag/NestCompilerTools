package org.nest.ast.generation.llvm.types;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.global.LLVM;


public class TypeFactory
{
    private final LLVMContextRef context;

    public TypeFactory(LLVMContextRef context)
    {
        this.context = context;
    }

    // 🧠 Static helper for reconstructing a Type from a raw LLVMTypeRef
    public static Type fromLLVM(LLVMTypeRef ref)
    {
        int kind = LLVM.LLVMGetTypeKind(ref);
        return switch (kind)
        {
            case LLVM.LLVMIntegerTypeKind -> new IntType(ref);
            case LLVM.LLVMPointerTypeKind -> new PointerType(ref);
            case LLVM.LLVMStructTypeKind -> new StructType(ref);
            case LLVM.LLVMArrayTypeKind -> new ArrayType(ref);
            case LLVM.LLVMFunctionTypeKind -> new FunctionType(ref);
            case LLVM.LLVMFloatTypeKind, LLVM.LLVMDoubleTypeKind -> new FloatType(ref);
            default -> throw new UnsupportedOperationException("Unsupported LLVM type kind: " + kind);
        };
    }

    public IntType i1()
    {
        return new IntType(LLVM.LLVMInt1TypeInContext(context));
    }

    public IntType i8()
    {
        return new IntType(LLVM.LLVMInt8TypeInContext(context));
    }

    public IntType i16()
    {
        return new IntType(LLVM.LLVMInt16TypeInContext(context));
    }

    public IntType i32()
    {
        return new IntType(LLVM.LLVMInt32TypeInContext(context));
    }

    public IntType i64()
    {
        return new IntType(LLVM.LLVMInt64TypeInContext(context));
    }

    public FloatType f32()
    {
        return new FloatType(LLVM.LLVMFloatTypeInContext(context));
    }

    public FloatType f64()
    {
        return new FloatType(LLVM.LLVMDoubleTypeInContext(context));
    }

    public PointerType pointerTo(Type base)
    {
        return new PointerType(LLVM.LLVMPointerType(base.getRef(), 0));
    }

    public ArrayType arrayOf(Type elementType, int count)
    {
        return new ArrayType(LLVM.LLVMArrayType2(elementType.getRef(), count));
    }

    public FunctionType function(Type returnType, Type[] params, boolean isVarArg)
    {
        PointerPointer<LLVMTypeRef> args = new PointerPointer<>(params.length);
        for (int i = 0; i < params.length; i++)
        {
            args.put(i, params[i].getRef());
        }
        return new FunctionType(LLVM.LLVMFunctionType(returnType.getRef(), args, params.length, isVarArg ? 1 : 0));
    }

    public StructType struct(String name, Type[] fields, boolean packed)
    {
        PointerPointer<LLVMTypeRef> members = new PointerPointer<>(fields.length);
        for (int i = 0; i < fields.length; i++)
        {
            members.put(i, fields[i].getRef());
        }
        LLVMTypeRef struct = LLVM.LLVMStructCreateNamed(context, name);
        LLVM.LLVMStructSetBody(struct, members, fields.length, packed ? 1 : 0);
        return new StructType(struct);
    }
}
