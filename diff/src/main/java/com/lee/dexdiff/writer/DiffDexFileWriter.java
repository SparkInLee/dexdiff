package com.lee.dexdiff.writer;

import static com.googlecode.d2j.dex.writer.ev.EncodedValue.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.googlecode.d2j.DexType;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.dex.writer.CodeWriter;
import com.googlecode.d2j.dex.writer.DexFileWriter;
import com.googlecode.d2j.dex.writer.CodeWriter.IndexedInsn;
import com.googlecode.d2j.dex.writer.CodeWriter.OP35c;
import com.googlecode.d2j.dex.writer.CodeWriter.OP3rc;
import com.googlecode.d2j.dex.writer.CodeWriter.PackedSwitch;
import com.googlecode.d2j.dex.writer.CodeWriter.SparseSwitch;
import com.googlecode.d2j.dex.writer.ev.EncodedAnnotation;
import com.googlecode.d2j.dex.writer.ev.EncodedArray;
import com.googlecode.d2j.dex.writer.ev.EncodedValue;
import com.googlecode.d2j.dex.writer.ev.EncodedAnnotation.AnnotationElement;
import com.googlecode.d2j.dex.writer.insn.Insn;
import com.googlecode.d2j.dex.writer.insn.JumpOp;
import com.googlecode.d2j.dex.writer.insn.Label;
import com.googlecode.d2j.dex.writer.insn.PreBuildInsn;
import com.googlecode.d2j.dex.writer.item.AnnotationItem;
import com.googlecode.d2j.dex.writer.item.AnnotationSetItem;
import com.googlecode.d2j.dex.writer.item.BaseItem;
import com.googlecode.d2j.dex.writer.item.ClassDataItem;
import com.googlecode.d2j.dex.writer.item.ClassDefItem;
import com.googlecode.d2j.dex.writer.item.CodeItem;
import com.googlecode.d2j.dex.writer.item.DebugInfoItem;
import com.googlecode.d2j.dex.writer.item.FieldIdItem;
import com.googlecode.d2j.dex.writer.item.MethodIdItem;
import com.googlecode.d2j.dex.writer.item.StringIdItem;
import com.googlecode.d2j.dex.writer.item.TypeIdItem;
import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedField;
import com.googlecode.d2j.dex.writer.item.ClassDataItem.EncodedMethod;
import com.googlecode.d2j.dex.writer.item.CodeItem.EncodedCatchHandler.AddrPair;
import com.googlecode.d2j.dex.writer.item.CodeItem.TryItem;
import com.googlecode.d2j.visitors.DexAnnotationAble;
import com.googlecode.d2j.visitors.DexAnnotationVisitor;
import com.googlecode.d2j.visitors.DexClassVisitor;
import com.googlecode.d2j.visitors.DexDebugVisitor;
import com.googlecode.d2j.visitors.DexFieldVisitor;
import com.googlecode.d2j.visitors.DexMethodVisitor;
import com.lee.dexdiff.utils.DiffUtils;
import com.lee.dexdiff.utils.Logger;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffDexFileWriter {
    private static final String TAG = "DiffDexFileWriter";

    private DexFileWriter mFw;

    private boolean discardDebug = true;

    private static DiffDexFileWriter sInstance;

    public static DiffDexFileWriter getInstance() {
        if (null == sInstance) {
            synchronized (DiffDexFileWriter.class) {
                if (null == sInstance) {
                    sInstance = new DiffDexFileWriter();
                }
            }
        }
        return sInstance;
    }

    private DiffDexFileWriter() {

    }

    public void discardDebug() {
        discardDebug = true;
    }

    public void keepDebug() {
        discardDebug = false;
    }

    public void build(Collection<ClassDefItem> values, File dexFile) throws IOException {
        if (null == dexFile) {
            Logger.e(TAG, "Failed to write dex : dexFile is null.");
            return;
        }
        if (dexFile.exists()) {
            Logger.e(TAG, String.format("Failed to write %s : dexFile exists.", dexFile.getName()));
            return;
        }
        if (null == values || values.size() <= 0) {
            Logger.e(TAG, String.format("Failed to write %s : values is null or empty.", dexFile.getName()));
            return;
        }

        mFw = new DexFileWriter();
        for (ClassDefItem classItem : values) {
            int accessFlags = classItem.accessFlags;
            String className = classItem.clazz.descriptor.stringData.string;
            String superClass = null;
            if (null != classItem.superclazz) {
                superClass = classItem.superclazz.descriptor.stringData.string;
            }
            String[] interfaces = null;
            if (null != classItem.interfaces) {
                interfaces = new String[classItem.interfaces.items.size()];
                for (int i = 0; i < interfaces.length; ++i) {
                    interfaces[i] = classItem.interfaces.items.get(i).descriptor.stringData.string;
                }
            }
            DexClassVisitor cw = mFw.visit(accessFlags, className, superClass, interfaces);
            if (null != classItem.sourceFile) {
                cw.visitSource(classItem.sourceFile.stringData.string);
            }
            // write annotation
            if (null != classItem.classAnnotations) {
                for (AnnotationItem annotationItem : classItem.classAnnotations.annotations) {
                    EncodedAnnotation annotation = annotationItem.annotation;
                    DexAnnotationVisitor aw = cw.visitAnnotation(annotation.type.descriptor.stringData.string,
                            annotationItem.visibility);
                    writeAnnotation(aw, annotation);
                    aw.visitEnd();
                }
            }

            // write class data
            if (null != classItem.classData) {
                ClassDataItem classDataItem = classItem.classData;
                // write static field
                if (null != classDataItem.staticFields) {
                    for (EncodedField encodedField : classDataItem.staticFields) {
                        writeField(cw, encodedField);
                    }
                }

                // write instance field
                if (null != classDataItem.instanceFields) {
                    for (EncodedField encodedField : classDataItem.instanceFields) {
                        writeField(cw, encodedField);
                    }
                }

                // write direct method
                if (null != classDataItem.directMethods) {
                    for (EncodedMethod encodedMethod : classDataItem.directMethods) {
                        writeMethod(cw, encodedMethod);
                    }
                }

                // write virtual method
                if (null != classDataItem.virtualMethods) {
                    for (EncodedMethod encodedMethod : classDataItem.virtualMethods) {
                        writeMethod(cw, encodedMethod);
                    }
                }
            }

            cw.visitEnd();
            Logger.d(TAG, "write class : " + classItem.clazz);
        }
        mFw.visitEnd();
        DiffUtils.toDexFile(mFw, dexFile);
    }

    private void writeAnnotation(DexAnnotationVisitor aw, EncodedAnnotation encodedAnnotation) {
        if (null != encodedAnnotation.elements) {
            for (AnnotationElement element : encodedAnnotation.elements) {
                String name = element.name.stringData.string;
                Object decodeValue = decodeValue(element.value);
                if (element.value.valueType == VALUE_ENUM) {
                    Field field = (Field) decodeValue;
                    aw.visitEnum(name, field.getOwner(), field.getName());
                } else if (decodeValue instanceof EncodedAnnotation) {
                    EncodedAnnotation aannotation = (EncodedAnnotation) decodeValue;
                    DexAnnotationVisitor aaw = aw.visitAnnotation(name, aannotation.type.descriptor.stringData.string);
                    writeAnnotation(aaw, aannotation);
                    aaw.visitEnd();
                } else if (decodeValue instanceof EncodedArray) {
                    DexAnnotationVisitor aaw = aw.visitArray(name);
                    writeAnnotationArrayValue(aaw, (EncodedArray) decodeValue);
                    aaw.visitEnd();
                } else if (decodeValue instanceof Object[]) {
                    Object[] values = (Object[]) decodeValue;
                    EncodedArray array = new EncodedArray();
                    for (Object value : values) {
                        array.values.add(EncodedValue.wrap(mFw.cp.wrapEncodedItem(value)));
                    }
                    DexAnnotationVisitor aaw = aw.visitArray(name);
                    writeAnnotationArrayValue(aaw, array);
                    aaw.visitEnd();
                } else {
                    aw.visit(name, decodeValue);
                }
            }
        }
    }

    private void writeAnnotationArrayValue(DexAnnotationVisitor aw, EncodedArray array) {
        for (EncodedValue value : array.values) {
            String name = null;
            Object decodeValue = decodeValue(value);
            if (value.valueType == VALUE_ENUM) {
                Field field = (Field) decodeValue;
                aw.visitEnum(name, field.getOwner(), field.getName());
            } else if (decodeValue instanceof EncodedAnnotation) {
                EncodedAnnotation aannotation = (EncodedAnnotation) decodeValue;
                DexAnnotationVisitor aaw = aw.visitAnnotation(name, aannotation.type.descriptor.stringData.string);
                writeAnnotation(aaw, aannotation);
                aaw.visitEnd();
            } else if (decodeValue instanceof EncodedArray) {
                DexAnnotationVisitor aaw = aw.visitArray(name);
                writeAnnotationArrayValue(aaw, (EncodedArray) decodeValue);
                aaw.visitEnd();
            } else if (decodeValue instanceof Object[]) {
                Object[] values = (Object[]) decodeValue;
                EncodedArray aarray = new EncodedArray();
                for (Object avalue : values) {
                    aarray.values.add(EncodedValue.wrap(mFw.cp.wrapEncodedItem(avalue)));
                }
                DexAnnotationVisitor aaw = aw.visitArray(name);
                writeAnnotationArrayValue(aaw, aarray);
                aaw.visitEnd();
            } else {
                aw.visit(name, decodeValue);
            }
        }
    }

    private void writeField(DexClassVisitor cw, EncodedField encodedField) {
        Field field = new Field(encodedField.field.clazz.descriptor.stringData.string,
                encodedField.field.name.stringData.string, encodedField.field.type.descriptor.stringData.string);
        Object decodeValue = null;
        if (null != encodedField.staticValue) {
            decodeValue = decodeValue(encodedField.staticValue);
        }
        DexFieldVisitor fw = cw.visitField(encodedField.accessFlags, field, decodeValue);
        if (null != encodedField.annotationSetItem) {
            for (AnnotationItem annotationItem : encodedField.annotationSetItem.annotations) {
                EncodedAnnotation annotation = annotationItem.annotation;
                DexAnnotationVisitor aw = fw.visitAnnotation(annotation.type.descriptor.stringData.string,
                        annotationItem.visibility);
                writeAnnotation(aw, annotation);
                aw.visitEnd();
            }
        }
        fw.visitEnd();
    }

    private void writeMethod(DexClassVisitor cw, EncodedMethod encodedMethod) {
        MethodIdItem methodIdItem = encodedMethod.method;
        String[] parameters = new String[]{};
        if (null != methodIdItem.proto.parameters && null != methodIdItem.proto.parameters.items) {
            parameters = new String[methodIdItem.proto.parameters.items.size()];
            int index = 0;
            for (TypeIdItem item : methodIdItem.proto.parameters.items) {
                parameters[index++] = item.descriptor.stringData.string;
            }
        }
        Method method = new Method(methodIdItem.clazz.descriptor.stringData.string, methodIdItem.name.stringData.string,
                parameters, methodIdItem.proto.ret.descriptor.stringData.string);
        DexMethodVisitor mw = cw.visitMethod(encodedMethod.accessFlags, method);

        // write annotation
        if (null != encodedMethod.annotationSetItem) {
            for (AnnotationItem annotationItem : encodedMethod.annotationSetItem.annotations) {
                EncodedAnnotation annotation = annotationItem.annotation;
                DexAnnotationVisitor aw = mw.visitAnnotation(annotation.type.descriptor.stringData.string,
                        annotationItem.visibility);
                writeAnnotation(aw, annotation);
                aw.visitEnd();
            }
        }

        // write parameter annotation
        if (null != encodedMethod.parameterAnnotation) {
            AnnotationSetItem[] annotationSetItems = encodedMethod.parameterAnnotation.annotationSets;
            if (null != annotationSetItems) {
                for (int i = 0; i < annotationSetItems.length; ++i) {
                    AnnotationSetItem annotationSetItem = annotationSetItems[i];
                    DexAnnotationAble parameterAw = mw.visitParameterAnnotation(i);
                    if (null != annotationSetItem && null != annotationSetItem.annotations) {
                        for (AnnotationItem annotationItem : annotationSetItem.annotations) {
                            EncodedAnnotation annotation = annotationItem.annotation;
                            DexAnnotationVisitor aw = parameterAw.visitAnnotation(
                                    annotation.type.descriptor.stringData.string, annotationItem.visibility);
                            writeAnnotation(aw, annotation);
                            aw.visitEnd();
                        }
                    }
                }
            }
        }

        // write code
        if (null != encodedMethod.code) {
            writeCode((CodeWriter) mw.visitCode(), encodedMethod.code);
        }

        mw.visitEnd();
    }

    private void writeCode(CodeWriter cw, CodeItem codeItem) {
        // register size
        cw.visitRegister(codeItem.registersSize);

        // find labels
        Map<Label, Label> labelMap = new HashMap<>();
        if (null != codeItem._ops && codeItem._ops.size() > 0) {
            for (Insn insn : codeItem._ops) {
                if (insn instanceof Label) {
                    labelMap.put((Label) insn, new Label());
                }
            }
        }
        if (null != codeItem._tailOps && codeItem._tailOps.size() > 0) {
            for (Insn insn : codeItem._tailOps) {
                if (insn instanceof Label) {
                    labelMap.put((Label) insn, new Label());
                }
            }
        }

        // find jump operations
        Map<JumpOp, JumpOp> jumpOpMap = new HashMap<>();
        if (null != codeItem._ops && codeItem._ops.size() > 0) {
            for (Insn insn : codeItem._ops) {
                if (insn instanceof JumpOp) {
                    JumpOp oldJumpOp = (JumpOp) insn;
                    jumpOpMap.put(oldJumpOp,
                            new JumpOp(oldJumpOp.op, oldJumpOp.a, oldJumpOp.b, getLabel(oldJumpOp.label, labelMap)));
                }
            }
        }
        if (null != codeItem._ops && codeItem._ops.size() > 0) {
            for (Insn insn : codeItem._ops) {
                if (insn instanceof JumpOp) {
                    JumpOp oldJumpOp = (JumpOp) insn;
                    jumpOpMap.put(oldJumpOp,
                            new JumpOp(oldJumpOp.op, oldJumpOp.a, oldJumpOp.b, getLabel(oldJumpOp.label, labelMap)));
                }
            }
        }

        // write operations
        if (null != codeItem._ops && codeItem._ops.size() > 0) {
            for (Insn insn : codeItem._ops) {
                writeInsn(insn, labelMap, jumpOpMap, cw.ops);
            }
        }

        // write tail operations
        if (null != codeItem._tailOps && codeItem._tailOps.size() > 0) {
            for (Insn insn : codeItem._tailOps) {
                writeInsn(insn, labelMap, jumpOpMap, cw.tailOps);
            }
        }

        // write try items
        if (null != codeItem._tryItems && codeItem._tryItems.size() > 0) {
            for (TryItem item : codeItem._tryItems) {
                int size = item.handler.addPairs.size();
                if (null != item.handler.catchAll) {
                    size += 1;
                }
                Label[] handles = new Label[size];
                String[] types = new String[size];
                int index = 0;
                for (AddrPair pair : item.handler.addPairs) {
                    handles[index] = getLabel(pair.addr, labelMap);
                    types[index] = pair.type.descriptor.stringData.string;
                    ++index;
                }
                if (null != item.handler.catchAll) {
                    handles[size - 1] = getLabel(item.handler.catchAll, labelMap);
                    types[size - 1] = null;
                }
                cw.visitTryCatch(getLabel(item.start, labelMap), getLabel(item.end, labelMap), handles, types);
            }
        }

        // eliminate debug info
        if (null != codeItem.debugInfo && !discardDebug) {
            writeDebugInfo(cw.visitDebug(), codeItem.debugInfo);
        }

        cw.in_reg_size = codeItem.insSize;
        cw.max_out_reg_size = codeItem.outsSize;

        cw.visitEnd();
    }

    private void writeDebugInfo(DexDebugVisitor dw, DebugInfoItem item) {
        // TODO
        dw.visitEnd();
    }

    private void writeInsn(Insn insn, Map<Label, Label> labelMap, Map<JumpOp, JumpOp> jumpOpMap, List<Insn> ops) {
        if (insn instanceof Label) {
            ops.add(getLabel((Label) insn, labelMap));
        } else if (insn instanceof PreBuildInsn) {
            ops.add(new PreBuildInsn(((PreBuildInsn) insn).data));
        } else if (insn instanceof JumpOp) {
            ops.add(getJumpOp((JumpOp) insn, jumpOpMap));
        } else if (insn instanceof SparseSwitch) {
            ops.add(newSparseSwitch((SparseSwitch) insn, labelMap, jumpOpMap));
        } else if (insn instanceof PackedSwitch) {
            ops.add(newPackedSwitch((PackedSwitch) insn, labelMap, jumpOpMap));
        } else if (insn instanceof OP35c) {
            BaseItem item = copyBaseItem(((OP35c) insn).item);
            int len = ((OP35c) insn).A;
            int[] args = new int[len];
            switch (len) {
                case 5:
                    args[4] = ((OP35c) insn).G;
                case 4:
                    args[3] = ((OP35c) insn).F;
                case 3:
                    args[2] = ((OP35c) insn).E;
                case 2:
                    args[1] = ((OP35c) insn).D;
                case 1:
                    args[0] = ((OP35c) insn).C;
                    break;
            }
            ops.add(new OP35c(((OP35c) insn).op, args, item));
        } else if (insn instanceof OP3rc) {
            BaseItem item = copyBaseItem(((OP3rc) insn).item);
            ops.add(new OP3rc(((OP3rc) insn).op, ((OP3rc) insn).start, ((OP3rc) insn).length, item));
        } else if (insn instanceof IndexedInsn) {
            BaseItem idxItem = copyBaseItem(((IndexedInsn) insn).idxItem);
            ops.add(new IndexedInsn(((IndexedInsn) insn).op, ((IndexedInsn) insn).a, ((IndexedInsn) insn).b, idxItem));
        } else {
            throw new IllegalArgumentException("unsupported operations");
        }
    }

    private Label getLabel(Label oldLabel, Map<Label, Label> labelMap) {
        if (labelMap.containsKey(oldLabel)) {
            return labelMap.get(oldLabel);
        } else {
            throw new IllegalStateException("No Such Label.");
        }
    }

    private JumpOp getJumpOp(JumpOp oldJumpOp, Map<JumpOp, JumpOp> jumpOpMap) {
        if (jumpOpMap.containsKey(oldJumpOp)) {
            return jumpOpMap.get(oldJumpOp);
        } else {
            throw new IllegalStateException("No Such Jump Operation.");
        }
    }

    private SparseSwitch newSparseSwitch(SparseSwitch oldSparseSwitch, Map<Label, Label> labelMap,
                                         Map<JumpOp, JumpOp> jumpOpMap) {
        Label[] oldLabels = oldSparseSwitch.labels;
        JumpOp oldJumpOp = oldSparseSwitch.jumpOp;
        Label[] newLabels = new Label[oldLabels.length];
        for (int i = 0; i < oldLabels.length; ++i) {
            newLabels[i] = getLabel(oldLabels[i], labelMap);
        }
        return new SparseSwitch(getJumpOp(oldJumpOp, jumpOpMap), oldSparseSwitch.cases, newLabels);
    }

    private PackedSwitch newPackedSwitch(PackedSwitch oldPackedSwitch, Map<Label, Label> labelMap,
                                         Map<JumpOp, JumpOp> jumpOpMap) {
        Label[] oldLabels = oldPackedSwitch.labels;
        JumpOp oldJumpOp = oldPackedSwitch.jumpOp;
        Label[] newLabels = new Label[oldLabels.length];
        for (int i = 0; i < oldLabels.length; ++i) {
            newLabels[i] = getLabel(oldLabels[i], labelMap);
        }
        return new PackedSwitch(getJumpOp(oldJumpOp, jumpOpMap), oldPackedSwitch.first_case, newLabels);
    }

    private BaseItem copyBaseItem(BaseItem item) {
        BaseItem newItem = null;
        if (item instanceof StringIdItem) {
            newItem = mFw.cp.uniqString(((StringIdItem) item).stringData.string);
        } else if (item instanceof FieldIdItem) {
            FieldIdItem fieldIdItem = (FieldIdItem) item;
            Field field = new Field(fieldIdItem.clazz.descriptor.stringData.string, fieldIdItem.name.stringData.string,
                    fieldIdItem.type.descriptor.stringData.string);
            newItem = mFw.cp.uniqField(field);
        } else if (item instanceof MethodIdItem) {
            MethodIdItem methodIdItem = (MethodIdItem) item;
            String[] parameters = new String[]{};
            if (null != methodIdItem.proto.parameters && null != methodIdItem.proto.parameters.items) {
                parameters = new String[methodIdItem.proto.parameters.items.size()];
                int index = 0;
                for (TypeIdItem typeIdItem : methodIdItem.proto.parameters.items) {
                    parameters[index++] = typeIdItem.descriptor.stringData.string;
                }
            }
            Method method = new Method(methodIdItem.clazz.descriptor.stringData.string,
                    methodIdItem.name.stringData.string, parameters,
                    methodIdItem.proto.ret.descriptor.stringData.string);
            newItem = mFw.cp.uniqMethod(method);
        } else if (item instanceof TypeIdItem) {
            newItem = mFw.cp.uniqType(((TypeIdItem) item).descriptor.stringData.string);
        } else {
            throw new IllegalArgumentException("type of idxItem in IndexedInsn unsupported.");
        }
        return newItem;
    }

    private Object decodeValue(EncodedValue encodeValue) {
        Object decodeValue = null;
        switch (encodeValue.valueType) {
            case VALUE_NULL:
            case VALUE_BOOLEAN:
            case VALUE_BYTE:
            case VALUE_SHORT:
            case VALUE_CHAR:
            case VALUE_INT:
            case VALUE_LONG:
            case VALUE_DOUBLE:
            case VALUE_FLOAT:
                decodeValue = encodeValue.value;
                break;
            case VALUE_STRING:
                StringIdItem string = (StringIdItem) encodeValue.value;
                decodeValue = string.stringData.string;
                break;
            case VALUE_TYPE:
                TypeIdItem type = (TypeIdItem) encodeValue.value;
                decodeValue = new DexType(type.descriptor.stringData.string);
                break;
            case VALUE_FIELD:
                FieldIdItem field = (FieldIdItem) encodeValue.value;
                decodeValue = new Field(field.clazz.descriptor.stringData.string, field.name.stringData.string,
                        field.type.descriptor.stringData.string);
                break;
            case VALUE_METHOD:
                MethodIdItem method = (MethodIdItem) encodeValue.value;
                String[] parameters = new String[]{};
                if (null != method.proto.parameters && null != method.proto.parameters.items) {
                    parameters = new String[method.proto.parameters.items.size()];
                    int index = 0;
                    for (TypeIdItem item : method.proto.parameters.items) {
                        parameters[index++] = item.descriptor.stringData.string;
                    }
                }
                decodeValue = new Method(method.clazz.descriptor.stringData.string, method.name.stringData.string,
                        parameters, method.proto.ret.descriptor.stringData.string);
                break;
            case VALUE_ENUM:
                FieldIdItem encodeField = (FieldIdItem) encodeValue.value;
                decodeValue = new Field(encodeField.clazz.descriptor.stringData.string, encodeField.name.stringData.string,
                        encodeField.type.descriptor.stringData.string);
                break;
            case VALUE_ARRAY:
                decodeValue = (EncodedArray) encodeValue.value;
                break;
            case VALUE_ANNOTATION:
                decodeValue = (EncodedAnnotation) encodeValue.value;
                break;
            default:
                throw new RuntimeException();
        }
        return decodeValue;
    }
}
