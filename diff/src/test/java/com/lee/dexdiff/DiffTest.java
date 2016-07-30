package com.lee.dexdiff;

import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.lee.dexdiff.utils.DiffUtils;
import com.lee.dexdiff.utils.Logger;
import com.lee.dexdiff.utils.TwoTuple;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffTest extends Assert {

    @BeforeClass
    public static void init() {
        Logger.setLevel(3);
    }

    @Test
    public void testRewrite() {
        try {
            String oldFile = "src/test/resource/1.dex";
            String rewriteFile = DiffUtils.rewriteDex(oldFile);
            assertNull(DiffUtils.diff(oldFile, rewriteFile, null));
            new File(rewriteFile).delete();
        } catch (IOException e) {
            fail(stringify(e));
        }
    }

    @Test
    public void testDiffV1() {
        File keepFile = null;
        try {
            String oldFile = "src/test/resource/1.dex";
            String newFile = "src/test/resource/2.dex";
            TwoTuple<String, String> ret = DiffUtils.diff(oldFile, newFile, null);
            assertNotNull(ret.first);
            assertNull(ret.second);

            keepFile = new File(ret.first);
            DexFileNode fileNode = new DexFileNode();
            DexFileReader reader = new DexFileReader(keepFile);
            reader.accept(fileNode);
            assertNotNull(fileNode.clzs);
            assertEquals("keep class is wrong.", 1, fileNode.clzs.size());
        } catch (IOException e) {
            fail(stringify(e));
        } finally {
            if (null != keepFile) {
                keepFile.delete();
            }
        }
    }

    @Test
    public void testDiffV2() {
        File keepFile = null;
        File deleteFile = null;
        try {
            String oldFile = "src/test/resource/1.dex";
            String newFile = "src/test/resource/3.dex";
            TwoTuple<String, String> ret = DiffUtils.diff(oldFile, newFile, null);
            assertNotNull(ret.first);
            assertNotNull(ret.second);

            keepFile = new File(ret.first);
            DexFileNode keepFileNode = new DexFileNode();
            DexFileReader keepReader = new DexFileReader(keepFile);
            keepReader.accept(keepFileNode);
            assertNotNull(keepFileNode.clzs);
            assertEquals("keep class is wrong.", 1, keepFileNode.clzs.size());

            deleteFile = new File(ret.second);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(deleteFile)));
            int count = 0;
            while (reader.readLine() != null) {
                ++count;
            }
            assertEquals("delete class is wrong.", 1, count);
        } catch (IOException e) {
            fail(stringify(e));
        } finally {
            if (null != keepFile) {
                keepFile.delete();
            }
            if (null != deleteFile) {
                deleteFile.delete();
            }
        }
    }

    @Test
    public void testPatch() {
        String keepFile = null;
        String deleteFile = null;
        String newDex = null;
        try {
            String oldFile = "src/test/resource/1.dex";
            String newFile = "src/test/resource/3.dex";
            TwoTuple<String, String> ret = DiffUtils.diff(oldFile, newFile, null);
            assertNotNull(ret);
            keepFile = ret.first;
            deleteFile = ret.second;
            newDex = DiffUtils.patch(oldFile, ret.first, ret.second);
            assertNull(DiffUtils.diff(newFile, newDex, null));
        } catch (IOException e) {
            fail(stringify(e));
        } finally {
            if (null != keepFile) {
                new File(keepFile).delete();
            }
            if (null != deleteFile) {
                new File(deleteFile).delete();
            }
            if (null != newDex) {
                new File(newDex).delete();
            }
        }
    }

    private String stringify(Throwable e) {
        return null == e ? "null" : "{class=" + e.getClass().getSimpleName() + ", message=" + e.getMessage() + "}";
    }
}
