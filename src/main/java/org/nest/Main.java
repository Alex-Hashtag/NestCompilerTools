package org.nest;

import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import org.bytedeco.javacpp.BytePointer;
import org.nest.ast.generation.llvm.*;
import org.nest.ast.generation.llvm.types.Type;
import org.nest.ast.generation.llvm.types.StructType;
import org.nest.ast.generation.llvm.types.ArrayType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting LLVM IR generation with struct types demonstration");

        try {
            System.out.println("Initializing LLVM...");

            Context context = Context.create();
            LLVMContextRef contextRef = context.getContextRef();
            LLVMModuleRef moduleRef = context.getModuleRef();

            try {
                // Define the main function (int main())
                Type returnType = context.types().i32();
                Type[] mainParams = new Type[0];
                Function mainFunction = context.defineFunction("main", returnType, mainParams, false);
                System.out.println("Created main function");

                // Create entry block and builder
                IRBuilder builder = mainFunction.getEntryBuilder();
                BasicBlock entryBlock = mainFunction.getEntryBlock();
                builder.moveToBlock(entryBlock);
                
                // Step 1: Define Address struct type
                Type[] addressFields = new Type[] {
                    // String fields will be char arrays
                    context.types().arrayOf(context.types().i8(), 100), // street: char[100]
                    context.types().arrayOf(context.types().i8(), 50),  // city: char[50]
                    context.types().arrayOf(context.types().i8(), 20),  // zipCode: char[20]
                };
                StructType addressType = context.types().struct("Address", addressFields, false);
                System.out.println("Defined Address struct type");
                
                // Step 2: Define Person struct type with a nested Address
                Type[] personFields = new Type[] {
                    context.types().arrayOf(context.types().i8(), 50),  // name: char[50]
                    context.types().i32(),                              // age: int32
                    addressType,                                        // address: Address
                    context.types().i1()                                // isEmployed: bool
                };
                StructType personType = context.types().struct("Person", personFields, false);
                System.out.println("Defined Person struct type with nested Address");
                
                // Step 3: Allocate a Person instance on the stack
                Value personVar = builder.allocateStack(personType, "person");
                System.out.println("Allocated Person instance on stack");
                
                // Step 4: Set the person's age to 30
                Value agePtr = builder.getStructFieldPointer(personVar, 1, "age.ptr");
                builder.storeValue(context.types().i32().createValue(30), agePtr);
                System.out.println("Set person's age to 30");
                
                // Step 5: Set isEmployed to true
                Value isEmployedPtr = builder.getStructFieldPointer(personVar, 3, "employed.ptr");
                builder.storeValue(context.types().i1().createValue(1), isEmployedPtr);
                System.out.println("Set person's employment status to true");
                
                // Step 6: Get a pointer to the nested Address struct
                Value addressPtr = builder.getStructFieldPointer(personVar, 2, "addr.ptr");
                
                // Step 7: Set the zipCode field of the Address
                // First get a pointer to the zipCode field (3rd field of Address)
                Value zipCodePtr = builder.getStructFieldPointer(addressPtr, 2, "zip.ptr");
                
                // Now we'd need helper functions to set string values properly, but for simplicity,
                // we'll just demonstrate by setting the first character
                Value firstCharPtr = builder.getStructFieldPointer(
                    zipCodePtr, 0, "first.char.ptr");
                builder.storeValue(context.types().i8().createValue(9), firstCharPtr);
                
                // Step 8: Create a function to print Person info
                // Define a function to "print" a Person struct (in real code, would call printf)
                Type[] printPersonParams = new Type[] { personType };
                Function printPersonFunc = context.defineFunction(
                    "print_person", context.types().i32(), printPersonParams, false);
                BasicBlock printEntryBlock = printPersonFunc.getEntryBlock();
                IRBuilder printBuilder = printPersonFunc.getEntryBuilder();
                printBuilder.moveToBlock(printEntryBlock);
                
                // Function parameter (the person struct)
                Value personParam = new Value(LLVM.LLVMGetParam(printPersonFunc.getRef(), 0));
                
                // Get age from person parameter
                Value paramAgePtr = printBuilder.getStructFieldPointer(
                    personParam, 1, "param.age.ptr");
                Value paramAge = printBuilder.loadValue(
                    context.types().i32(), paramAgePtr, "param.age");
                
                // Get isEmployed from person parameter
                Value paramIsEmployedPtr = printBuilder.getStructFieldPointer(
                    personParam, 3, "param.employed.ptr");
                Value paramIsEmployed = printBuilder.loadValue(
                    context.types().i1(), paramIsEmployedPtr, "param.employed");
                
                // In a real function, we would print these values
                // Here we'll just return the age value
                printBuilder.returnValue(paramAge);
                printBuilder.dispose();
                
                // Step 9: Create another Person and compare the ages
                Value personVar2 = builder.allocateStack(personType, "person2");
                Value age2Ptr = builder.getStructFieldPointer(personVar2, 1, "age2.ptr");
                builder.storeValue(context.types().i32().createValue(25), age2Ptr);
                
                // Load ages for comparison
                Value age1 = builder.loadValue(context.types().i32(), agePtr, "age1");
                Value age2 = builder.loadValue(context.types().i32(), age2Ptr, "age2");
                
                // Compare ages
                Value ageComparison = builder.compareGreaterThan(age1, age2, "age.gt");
                
                // Create blocks for the comparison result
                BasicBlock olderBlock = mainFunction.createBlock("older.person");
                BasicBlock youngerBlock = mainFunction.createBlock("younger.person");
                BasicBlock exitBlock = mainFunction.createBlock("exit.block");
                
                // Branch based on the comparison
                builder.branchConditional(ageComparison, olderBlock, youngerBlock);
                
                // If person1 is older
                builder.moveToBlock(olderBlock);
                // In real code, we might call printf to print a message
                builder.jumpToBlock(exitBlock);
                
                // If person2 is older or same age
                builder.moveToBlock(youngerBlock);
                // In real code, we might call printf to print a different message
                builder.jumpToBlock(exitBlock);
                
                // Exit block - return a success code
                builder.moveToBlock(exitBlock);
                builder.returnValue(context.types().i32().createValue(0));
                
                builder.dispose();

                // Print and save the generated LLVM IR
                BytePointer irPointer = LLVM.LLVMPrintModuleToString(moduleRef);
                String llvmIR = irPointer.getString();
                System.out.println("\nGenerated LLVM IR:");
                System.out.println(llvmIR);

                // Save the LLVM IR to a file
                String llFilePath = "output.ll";
                try (PrintWriter out = new PrintWriter(llFilePath)) {
                    out.println(llvmIR);
                    System.out.println("Saved IR to " + Paths.get(llFilePath).toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Error saving LLVM IR to file: " + e.getMessage());
                }


            } finally {
                LLVM.LLVMDisposeModule(moduleRef);
                LLVM.LLVMContextDispose(contextRef);
            }

            System.out.println("\nLLVM IR generation completed");

        } catch (Throwable e) {
            System.err.println("Uncaught error: ");
            e.printStackTrace();
        }
    }
}