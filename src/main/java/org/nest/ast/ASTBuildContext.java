package org.nest.ast;

import java.util.List;
import java.util.function.Supplier;


public interface ASTBuildContext
{
    <T> Runnable put(String key, T value);

    <T> T get(String key, Class<T> clazz);

    <T> T get(String key);

    <T> List<T> getList(String key, Class<T> clazz);

    <T> T getOrDefault(String key, T fallback);

    <T> T getOrElse(String key, Supplier<T> fallback);

    boolean has(String key);
}
