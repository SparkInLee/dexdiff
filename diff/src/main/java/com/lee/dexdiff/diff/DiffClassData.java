package com.lee.dexdiff.diff;

/**
 * Created by jianglee on 7/30/16.
 */
import com.googlecode.d2j.dex.writer.item.ClassDataItem;
import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedField;
import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedMethod;
import com.lee.dexdiff.func.Function1;
import com.lee.dexdiff.utils.DiffUtils;
import com.lee.dexdiff.utils.Logger;

public class DiffClassData implements Diff<ClassDataItem> {
    private static final String TAG = "DiffClassData";
    public static final DiffClassData sDefault = new DiffClassData();

    @Override
    public boolean diff(ClassDataItem oldValue, ClassDataItem newValue) {
        if (null == oldValue || null == newValue) {
            throw new IllegalArgumentException("classDataItem can not be null when diff.");
        }

        // static fields
        if (DiffUtils.diff(oldValue.staticFields, newValue.staticFields, new Function1<EncodedField, String>() {

            @Override
            public String apply(EncodedField t) {
                return t.field.toString();
            }
        }, DiffField.sDefault)) {
            Logger.d(TAG, "static fields are different -> " + newValue);
            return true;
        }

        // instance fields
        if (DiffUtils.diff(oldValue.instanceFields, newValue.instanceFields, new Function1<EncodedField, String>() {

            @Override
            public String apply(EncodedField t) {
                return t.field.toString();
            }
        }, DiffField.sDefault)) {
            Logger.d(TAG, "instance fields are different -> " + newValue);
            return true;
        }

        // direct methods
        if (DiffUtils.diff(oldValue.directMethods, newValue.directMethods, new Function1<EncodedMethod, String>() {

            @Override
            public String apply(EncodedMethod t) {
                return t.method.toString();
            }
        }, DiffMethod.sDefault)) {
            Logger.d(TAG, "direct methods are different -> " + newValue);
            return true;
        }

        // virtual methods
        if (DiffUtils.diff(oldValue.virtualMethods, newValue.virtualMethods, new Function1<EncodedMethod, String>() {

            @Override
            public String apply(EncodedMethod t) {
                return t.method.toString();
            }
        }, DiffMethod.sDefault)) {
            Logger.d(TAG, "virtual methods are different -> " + newValue);
            return true;
        }

        return false;
    }

}
