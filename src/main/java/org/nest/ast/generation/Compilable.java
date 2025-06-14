package org.nest.ast.generation;

import org.nest.ast.generation.llvm.Context;


public interface Compilable
{
     void generate(LLVMPipelineComponent context);
}
