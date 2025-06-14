package org.nest.ast.generation.llvm;


import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.nest.ast.generation.llvm.types.Type;

import static org.bytedeco.llvm.global.LLVM.*;

public class IRBuilder {
    private final LLVMBuilderRef builder;

    IRBuilder(LLVMContextRef ctx) {
        builder = LLVMCreateBuilderInContext(ctx);
    }

    /*──────── Positioning ───────*/
    public void moveToBlock(BasicBlock block) {
        LLVMPositionBuilderAtEnd(builder, block.getRef());
    }

    /*──────── Constants ─────────*/
    public Value constantInt(Type type, long value) {
        return new Value(LLVMConstInt(type.getRef(), value, 0));
    }

    /*──────── Memory ops ────────*/
    public Value allocateStack(Type type, String name) {
        return new Value(LLVMBuildAlloca(builder, type.getRef(), name));
    }

    public Value loadValue(Type type, Value ptr, String name) {
        return new Value(LLVMBuildLoad2(builder, type.getRef(), ptr.getRef(), name));
    }
    public Value storeValue(Value value, Value ptr) {
        return new Value(LLVMBuildStore(builder, value.getRef(), ptr.getRef()));
    }

    /*──────── Arithmetic ────────*/
    public Value addValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildAdd(builder, lhs.getRef(), rhs.getRef(), name));
    }

    public Value subtractValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildSub(builder, lhs.getRef(), rhs.getRef(), name));
    }

    public Value multiplyValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildMul(builder, lhs.getRef(), rhs.getRef(), name));
    }

    // Integer division
    public Value divideValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildSDiv(builder, lhs.getRef(), rhs.getRef(), name));
    }

    // Bitwise AND
    public Value andValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildAnd(builder, lhs.getRef(), rhs.getRef(), name));
    }

    // Bitwise OR
    public Value orValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildOr(builder, lhs.getRef(), rhs.getRef(), name));
    }

    // Bitwise XOR
    public Value xorValues(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildXor(builder, lhs.getRef(), rhs.getRef(), name));
    }

    /*──────── Comparisons ───────*/
    public Value compareGreaterThan(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntSGT, lhs.getRef(), rhs.getRef(), name)
        );
    }

    public Value compareEqual(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntEQ, lhs.getRef(), rhs.getRef(), name));
    }

    public Value compareNotEqual(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntNE, lhs.getRef(), rhs.getRef(), name));
    }

    public Value compareLessThan(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntSLT, lhs.getRef(), rhs.getRef(), name));
    }

    public Value compareLessOrEqual(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntSLE, lhs.getRef(), rhs.getRef(), name));
    }

    public Value compareGreaterOrEqual(Value lhs, Value rhs, String name) {
        return new Value(LLVMBuildICmp(builder, LLVMIntSGE, lhs.getRef(), rhs.getRef(), name));
    }

    public Value notValue(Value value, String name) {
        return new Value(LLVMBuildNot(builder, value.getRef(), name));
    }

    public Value zeroExtend(Value value, Type targetType, String name) {
        return new Value(LLVMBuildZExt(builder, value.getRef(), targetType.getRef(), name));
    }

    public Value signExtend(Value value, Type targetType, String name) {
        return new Value(LLVMBuildSExt(builder, value.getRef(), targetType.getRef(), name));
    }

    public Value truncate(Value value, Type targetType, String name) {
        return new Value(LLVMBuildTrunc(builder, value.getRef(), targetType.getRef(), name));
    }


    /*──────── Control flow ──────*/
    public void branchConditional(Value condition,
                                  BasicBlock thenBlock,
                                  BasicBlock elseBlock) {
        LLVMBuildCondBr(builder, condition.getRef(),
                thenBlock.getRef(), elseBlock.getRef());
    }

    public void jumpToBlock(BasicBlock target) {
        LLVMBuildBr(builder, target.getRef());
    }

    public void returnValue(Value value) {
        LLVMBuildRet(builder, value.getRef());
    }

    public void returnVoid() {
        LLVMBuildRetVoid(builder);
    }

    public void unreachable() {
        LLVMBuildUnreachable(builder);
    }

    public Value callFunction(Value function, Value[] args, String name) {
        PointerPointer<LLVMValueRef> argsPointer = new PointerPointer<>(args.length);
        for (int i = 0; i < args.length; i++) {
            argsPointer.put(i, args[i].getRef());
        }
        return new Value(LLVMBuildCall2(builder,
                LLVMGetElementType(LLVMTypeOf(function.getRef())), // function type
                function.getRef(),
                argsPointer,
                args.length,
                name));
    }


    /*──────── Struct helpers ────*/
    public Value getStructFieldPointer(Value structPtr, int index, String name) {
        LLVMValueRef gep = LLVMBuildStructGEP2(
                builder,
                LLVMTypeOf(structPtr.getRef()),
                structPtr.getRef(),
                index,
                name
        );
        return new Value(gep);
    }

    /*──────── Phi helper ────────*/
    public Value selectFromBranches(Type type,
                                    Value[] incomingValues,
                                    BasicBlock[] incomingBlocks,
                                    String name) {
        LLVMValueRef phi = LLVMBuildPhi(builder, type.getRef(), name);
        PointerPointer<LLVMBasicBlockRef> blocks =
                new PointerPointer<>(incomingBlocks.length);
        PointerPointer<LLVMValueRef> values =
                new PointerPointer<>(incomingValues.length);

        for (int i = 0; i < incomingBlocks.length; i++) {
            blocks.put(i, incomingBlocks[i].getRef());
            values.put(i, incomingValues[i].getRef());
        }
        LLVMAddIncoming(phi, values, blocks, incomingBlocks.length);
        return new Value(phi);
    }

    /*──────── Cleanup ───────────*/
    public void dispose() { LLVMDisposeBuilder(builder); }
}