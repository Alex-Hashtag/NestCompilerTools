package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import org.nest.ast.generation.llvm.Value;


/**
 *
 */
public class IntType extends Type
{
    public IntType(LLVMTypeRef ref)
    {
        super(ref);
    }

    @Override
    public TypeKind getKind()
    {
        return TypeKind.INT;
    }

    @Override
    public Value createValue(Object value)
    {
        if (value instanceof Integer intValue)
        {
            LLVMValueRef valueRef = LLVM.LLVMConstInt(ref, intValue, 0);
            return new Value(valueRef);
        }
        else if (value instanceof Long longValue)
        {
            LLVMValueRef valueRef = LLVM.LLVMConstInt(ref, longValue, 0);
            return new Value(valueRef);
        }
        else if (value instanceof String strValue)
        {
            try
            {
                long longValue = Long.parseLong(strValue);
                LLVMValueRef valueRef = LLVM.LLVMConstInt(ref, longValue, 0);
                return new Value(valueRef);
            } catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Cannot convert string to integer: " + strValue);
            }
        }
        throw new IllegalArgumentException("Cannot create integer value from: " + value.getClass().getName());
    }
}
