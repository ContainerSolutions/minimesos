package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for dealing with collections
 */
public class CollectionsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionsUtils.class);

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

    /**
     * This function split the cmd attribute in an array of String to make
     * it consumable by the withCmd from docker-java.
     *
     * It ensures that quotes and double quotes are correctly handled,
     * the split is performed on spaces.
    */
    public static String[] splitCmd(String cmd) {
        String arg = "";
        ArrayList<String> args = new ArrayList<String>();
        ArrayList<Character> quotes = new ArrayList<Character>();

        LOGGER.debug(String.format("Parsing cmd line: %s", cmd));
        for (int i = 0; i < cmd.length(); i++){
            char c = cmd.charAt(i);
            if (c == ' ' && quotes.size() == 0) { // split command on spaces
                args.add(arg);
                arg = "";
                continue;
            } else if (c == '\'' || c == '"') { // feed state array on quote and double quotes
                if (quotes.size() > 0 && quotes.get(0) == c) {
                    quotes.remove(0);
                } else {
                    quotes.add(0, c);
                }
            }
            arg += c;
        }
        // add last parsed elem
        if (arg != "") {
            args.add(arg);
        }
        // check unconsistent state
        if (quotes.size() != 0) {
            throw new MinimesosException("Marathon cmd config quotes are not closed properly");

        }
        return args.toArray(new String[args.size()]);
    }

}
