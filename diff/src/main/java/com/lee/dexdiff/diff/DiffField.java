package com.lee.dexdiff.diff;

import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedField;
import com.lee.dexdiff.utils.Logger;

import static com.lee.dexdiff.utils.DiffUtils.objectEqual;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffField implements Diff<EncodedField> {
    private static final String TAG = "DiffField";
    public static final DiffField sDefault = new DiffField();

    @Override
    public boolean diff(EncodedField oldValue, EncodedField newValue) {
        if (null == oldValue || null == newValue) {
            throw new IllegalArgumentException("encodeField can not be null when diff.");
        }

        // access
        if (oldValue.accessFlags != newValue.accessFlags) {
            Logger.d(TAG, "access is different -> " + newValue.field);
            return true;
        }

        // staticValue
        if (!objectEqual(oldValue.staticValue, newValue.staticValue)) {
            Logger.d(TAG, "staticValue is different -> " + newValue.field);
            return true;
        }

        // field annotations
        if (!objectEqual(oldValue.annotationSetItem, newValue.annotationSetItem)) {
            Logger.d(TAG, "annotations is different -> " + newValue.field);
            return true;
        }

        return false;
    }

}
