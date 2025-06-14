package org.nest.ast.generation.llvm.types;

import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;
import org.nest.ast.generation.llvm.Value;

/**
 * 
 */
public class FloatType extends Type
{
    public FloatType(LLVMTypeRef ref)
    {
        super(ref);
    }

    @Override
    public TypeKind getKind()
    {
        return TypeKind.FLOAT;
    }
    
    @Override
    public Value createValue(Object value) {
        if (value instanceof Float floatValue) {
            LLVMValueRef valueRef = LLVM.LLVMConstReal(ref, floatValue);
            return new Value(valueRef);
        } else if (value instanceof Double doubleValue) {
            LLVMValueRef valueRef = LLVM.LLVMConstReal(ref, doubleValue);
            return new Value(valueRef);
        } else if (value instanceof String strValue) {
            try {
                double doubleValue = Double.parseDouble(strValue);
                LLVMValueRef valueRef = LLVM.LLVMConstReal(ref, doubleValue);
                return new Value(valueRef);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert string to float: " + strValue);
            }
        }
        throw new IllegalArgumentException("Cannot create float value from: " + value.getClass().getName());
    }
}
