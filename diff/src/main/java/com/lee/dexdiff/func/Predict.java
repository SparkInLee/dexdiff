package com.lee.dexdiff.func;

/**
 * Created by jianglee on 7/30/16.
 */
public interface Predict<T> {

    boolean apply(T t);

    public final class PredictUtils {
        public static <V> Predict<V> TRUE() {
            return new Predict<V>() {

                @Override
                public boolean apply(V t) {
                    return true;
                }

            };
        }

        public static <V> Predict<V> FALSE() {
            return new Predict<V>() {

                @Override
                public boolean apply(V t) {
                    return false;
                }

            };
        }
    }
}
