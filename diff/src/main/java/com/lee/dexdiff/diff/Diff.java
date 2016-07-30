package com.lee.dexdiff.diff;

/**
 * Created by jianglee on 7/30/16.
 */
public interface Diff<T> {
    /**
     * @param oldValue non null
     * @param newValue non null
     * @return true means different
     */
    boolean diff(T oldValue, T newValue);
}
