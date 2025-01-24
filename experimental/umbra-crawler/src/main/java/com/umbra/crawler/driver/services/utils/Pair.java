package com.umbra.crawler.driver.services.utils;

/**
 * Based on Peter Norvig jscheme.
 */
public class Pair<T, S> {

    private final T first;

    private final S second;

    public Pair(T f, S s) {
        first = f;
        second = s;
    }

    public T getFirst() {
        return first;
    }
    public S getSecond() {
        return second;
    }

    public String toString() {
        return "("
                + ((first  == null) ? "" : first.toString()) + ", "
                + ((second == null) ? "" : second.toString()) + ")";
    }

    public <T> Pair<T, T> duplicate(T value) {

        return new Pair<T, T>(value, value);
    }

}