package org.apache.mesos.mini.util;

public interface Predicate<T> {
    boolean test(T t);
}