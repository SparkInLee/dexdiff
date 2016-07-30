package com.lee.dexdiff.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import com.googlecode.d2j.dex.writer.DexFileWriter;
import com.googlecode.d2j.dex.writer.item.ClassDefItem;
import com.googlecode.d2j.dex.writer.item.TypeIdItem;
import com.googlecode.d2j.reader.DexFileReader;
import com.lee.dexdiff.diff.DiffDexClass;
import com.lee.dexdiff.diff.Diff;
import com.lee.dexdiff.func.Function1;
import com.lee.dexdiff.func.Predict;
import com.lee.dexdiff.func.Predict.PredictUtils;
import com.lee.dexdiff.writer.DiffDexFileWriter;

/**
 * Created by jianglee on 7/30/16.
 */
public final class DiffUtils {
    private static final String TAG = "DiffUtils";

    public static TwoTuple<String, String> diff(String oldDexFile, String newDexFile, Predict<ClassDefItem> predict) throws IOException {
        if (null == predict) {
            predict = PredictUtils.TRUE();
        }

        DexFileWriter oldWriter = new DexFileWriter();
        DexFileReader reader = new DexFileReader(new File(oldDexFile));
        reader.accept(oldWriter);

        DexFileWriter newWriter = new DexFileWriter();
        reader = new DexFileReader(new File(newDexFile));
        reader.accept(newWriter);

        List<ClassDefItem> replaceValues = new ArrayList<>();
        List<ClassDefItem> deleteValues = new ArrayList<>();
        boolean isDiff = diff(oldWriter.cp.classDefs.values(), newWriter.cp.classDefs.values(), replaceValues,
                deleteValues, predict, new Function1<ClassDefItem, TypeIdItem>() {

                    @Override
                    public TypeIdItem apply(ClassDefItem t) {
                        return t.clazz;
                    }
                }, DiffDexClass.sDefault);

        if (isDiff) {
            Logger.i(TAG, String.format("%s is different from %s.", newDexFile, oldDexFile));
            TwoTuple<String, String> ret = new TwoTuple<>();
            if (replaceValues.size() > 0) {
                String keepFile = uniqueFileName("replace.dex", ".");
                DiffDexFileWriter.getInstance().build(replaceValues, new File(keepFile));
                ret.first = keepFile;
            }
            if (deleteValues.size() > 0) {
                String deleteFile = uniqueFileName("delete.dex", ".");
                DiffDexFileWriter.getInstance().build(deleteValues, new File(deleteFile));
                ret.second = deleteFile;
            }
            return ret;
        } else {
            Logger.i(TAG, String.format("%s is same with %s.", newDexFile, oldDexFile));
            return null;
        }
    }

    public static String rewriteDex(String oldDex) throws IOException {
        DexFileWriter oldWriter = new DexFileWriter();
        DexFileReader reader = new DexFileReader(new File(oldDex));
        reader.accept(oldWriter);
        String rewriteDex = uniqueFileName("rewrite.dex", ".");
        DiffDexFileWriter.getInstance().build(oldWriter.cp.classDefs.values(), new File(rewriteDex));
        return rewriteDex;
    }

    private static String uniqueFileName(String fileName, String split) {
        File file = new File(fileName);
        int suffix = 1;
        while (file.exists()) {
            file = new File(fileName.substring(0, fileName.lastIndexOf(split)) + suffix
                    + fileName.substring(fileName.lastIndexOf(split)));
            ++suffix;
        }
        return file.getName();
    }

    public static void toDexFile(DexFileWriter fw, File dexFile) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(dexFile);
            outputStream.write(fw.toByteArray());
            outputStream.close();
            Logger.i(TAG, String.format("write %s successfully.", dexFile.getName()));
        } finally {
            if (null != outputStream) {
                outputStream.close();
            }
        }
    }

    public static <T, K> boolean diff(Collection<T> oldValues, Collection<T> newValues, Collection<T> keepValues,
                                      Collection<T> deleteValues, Predict<T> predict, Function1<T, K> keyFunc, Diff<T> diff) {
        Map<K, T> oldTMap = new HashMap<>();
        if (null != oldValues) {
            for (T oldValue : oldValues) {
                if (predict.apply(oldValue)) {
                    oldTMap.put(keyFunc.apply(oldValue), oldValue);
                }
            }
        }
        if (null != newValues) {
            for (T newValue : newValues) {
                if (predict.apply(newValue)) {
                    T oldValue = oldTMap.remove(keyFunc.apply(newValue));
                    if (null != oldValue) {
                        if (diff.diff(oldValue, newValue)) {
                            // update
                            keepValues.add(newValue);
                        }
                    } else {
                        // add
                        keepValues.add(newValue);
                    }
                }
            }
        }
        if (oldTMap.values().size() > 0) {
            for (T value : oldTMap.values()) {
                // delete
                deleteValues.add(value);
            }
        }
        return keepValues.size() > 0 || deleteValues.size() > 0;
    }

    public static <T, K> boolean diff(Collection<T> oldValues, Collection<T> newValues, Function1<T, K> keyFunc,
                                      Diff<T> diff) {
        Map<K, T> oldTMap = new HashMap<>();
        if (null != oldValues) {
            for (T oldValue : oldValues) {
                oldTMap.put(keyFunc.apply(oldValue), oldValue);
            }
        }
        if (null != newValues) {
            for (T newValue : newValues) {
                T oldValue = oldTMap.remove(keyFunc.apply(newValue));
                if (null != oldValue) {
                    if (diff.diff(oldValue, newValue)) {
                        // has update
                        return true;
                    }
                } else {
                    // has add
                    return true;
                }

            }
        }

        if (oldTMap.size() > 0) {
            // has delete
            return true;
        }

        // no change
        return false;
    }

    public static boolean objectEqual(Object l, Object r) {
        return null != l ? (null == r || l.equals(r)) : (null == r);
    }
}
