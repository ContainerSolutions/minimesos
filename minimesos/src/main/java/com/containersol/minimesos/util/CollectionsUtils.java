package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for dealing with collections
 */
public class CollectionsUtils {

    private CollectionsUtils() {
        // do not allow creation of instances
    }

    public static <T> List<T> typedList(List original, Class<T> clazz) {

        ArrayList<T> typed = new ArrayList<>(original.size());

        for (Object obj : original) {
            if ((obj == null) || clazz.isAssignableFrom(obj.getClass())) {
                typed.add(clazz.cast(obj));
            } else {
                throw new MinimesosException("Not possible to cast " + obj + " to " + clazz.getCanonicalName());
            }
        }

        return typed;

    }

}
