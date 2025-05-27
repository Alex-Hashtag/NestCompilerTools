package org.nest.ast;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ASTBuildContext
{
    HashMap<String, Object> context = new HashMap<>();

    public <T> Runnable put(String key, T value)
    {
        context.put(key, value);
        return () -> context.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz)
    {
        Object value = context.get(key);
        if (value == null || !clazz.isAssignableFrom(value.getClass()))
            return null;
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key)
    {
        return (T) context.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, Class<T> clazz)
    {
        Object value = context.get(key);
        if (!(value instanceof List<?> list))
            return List.of();

        return list.stream()
                .filter(item -> item != null && clazz.isAssignableFrom(item.getClass()))
                .map(item -> (T) item)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T fallback)
    {
        Object value = context.get(key);
        if (value == null)
        {
            return fallback;
        }

        if (fallback == null || fallback.getClass().isAssignableFrom(value.getClass()))
        {
            return (T) value;
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrElse(String key, Supplier<T> fallback)
    {
        Object value = context.get(key);
        if (value == null)
        {
            return fallback.get();
        }
        return (T) value;
    }

    public boolean has(String key)
    {
        return context.containsKey(key);
    }
}
