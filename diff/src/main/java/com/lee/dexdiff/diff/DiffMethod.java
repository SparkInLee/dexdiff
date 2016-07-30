package com.lee.dexdiff.diff;

import java.util.Arrays;

import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedMethod;
import com.lee.dexdiff.utils.Logger;

import static com.lee.dexdiff.utils.DiffUtils.objectEqual;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffMethod implements Diff<EncodedMethod> {
    private static final String TAG = "DiffMethod";
    public static final DiffMethod sDefault = new DiffMethod();

    @Override
    public boolean diff(EncodedMethod oldValue, EncodedMethod newValue) {
        if (null == oldValue || null == newValue) {
            throw new IllegalArgumentException("encodeMethod can not be null when diff.");
        }

        // access
        if (oldValue.accessFlags != newValue.accessFlags) {
            Logger.d(TAG, "access is different -> " + newValue.method);
            return true;
        }

        // method annotations
        if (!objectEqual(oldValue.annotationSetItem, newValue.annotationSetItem)) {
            Logger.d(TAG, "annotations is different -> " + newValue.method);
            return true;
        }

        // parameter annotations
        boolean isParameterAnnsDiff = false;
        if (null != oldValue.parameterAnnotation) {
            if (null != newValue.parameterAnnotation) {
                if (!Arrays.equals(oldValue.parameterAnnotation.annotationSets,
                        newValue.parameterAnnotation.annotationSets)) {
                    isParameterAnnsDiff = true;
                }
            } else {
                isParameterAnnsDiff = true;
            }
        } else if (null != newValue.parameterAnnotation) {
            isParameterAnnsDiff = true;
        }
        if (isParameterAnnsDiff) {
            Logger.d(TAG, "parameter annotations is different -> " + newValue.method);
            return true;
        }

        // code data
        boolean isCodeDiff = false;
        if (null != oldValue.code) {
            if (null != newValue.code) {
                isCodeDiff = DiffCode.sDefault.diff(oldValue.code, newValue.code);
            } else {
                isCodeDiff = true;
            }
        } else if (null != newValue.code) {
            isCodeDiff = true;
        }
        if (isCodeDiff) {
            Logger.d(TAG, "code is different -> " + newValue.method);
            return true;
        }

        return false;
    }

}
