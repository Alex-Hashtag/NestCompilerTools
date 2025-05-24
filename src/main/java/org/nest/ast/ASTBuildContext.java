package org.nest.ast;

public interface ASTBuildContext {
    <T> void put(String key, T value);
    <T> T get(String key, Class<T> clazz);
    Object get(String key);
    <T> List<T> getList(String key, Class<T> clazz);
    <T> T getOrDefault(String key, T fallback);
    <T> T getOrElse(String key, Supplier<T> fallback);
    boolean has(String key);
}
