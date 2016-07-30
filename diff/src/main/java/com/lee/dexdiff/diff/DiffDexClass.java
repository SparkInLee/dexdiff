package com.lee.dexdiff.diff;

import com.googlecode.d2j.dex.writer.item.ClassDefItem;
import com.lee.dexdiff.utils.Logger;

import static com.lee.dexdiff.utils.DiffUtils.objectEqual;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffDexClass implements Diff<ClassDefItem> {
    private static final String TAG = "DexClassDiff";
    public static final DiffDexClass sDefault = new DiffDexClass();

    @Override
    public boolean diff(ClassDefItem oldValue, ClassDefItem newValue) {
        if (null == oldValue || null == newValue) {
            throw new IllegalArgumentException("classDefItem can not be null when diff.");
        }

        // access
        if (oldValue.accessFlags != newValue.accessFlags) {
            Logger.d(TAG, "access is different -> " + newValue.clazz + "\n");
            return true;
        }

        // super class
        if (!objectEqual(oldValue.superclazz, newValue.superclazz)) {
            Logger.d(TAG, "superClass is different -> " + newValue.clazz + "\n");
            return true;
        }

        // interfaces
        if (!objectEqual(oldValue.interfaces, newValue.interfaces)) {
            Logger.d(TAG, "interfaceNames is different -> " + newValue.clazz + "\n");
            return true;
        }

        // source
        if (!objectEqual(oldValue.sourceFile, newValue.sourceFile)) {
            Logger.d(TAG, "source is different -> " + newValue.clazz + "\n");
            return true;
        }

        // class annotations
        if (!objectEqual(oldValue.classAnnotations, newValue.classAnnotations)) {
            Logger.d(TAG, "annotations is different -> " + newValue.clazz + "\n");
            return true;
        }

        // class data
        boolean isDataDiff = false;
        if (null != oldValue.classData) {
            if (null != newValue.classData) {
                isDataDiff = DiffClassData.sDefault.diff(oldValue.classData, newValue.classData);
            } else {
                isDataDiff = true;
            }
        } else if (null != newValue.classData) {
            isDataDiff = true;
        }
        if (isDataDiff) {
            Logger.d(TAG, "class data is different -> " + newValue.clazz + "\n");
            return true;
        }

        return false;
    }

}
