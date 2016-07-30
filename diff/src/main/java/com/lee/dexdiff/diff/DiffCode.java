package com.lee.dexdiff.diff;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.d2j.dex.writer.CodeWriter.IndexedInsn;
import com.googlecode.d2j.dex.writer.CodeWriter.OP35c;
import com.googlecode.d2j.dex.writer.CodeWriter.OP3rc;
import com.googlecode.d2j.dex.writer.CodeWriter.PackedSwitch;
import com.googlecode.d2j.dex.writer.CodeWriter.SparseSwitch;
import com.googlecode.d2j.dex.writer.insn.Insn;
import com.googlecode.d2j.dex.writer.insn.JumpOp;
import com.googlecode.d2j.dex.writer.insn.Label;
import com.googlecode.d2j.dex.writer.insn.PreBuildInsn;
import com.googlecode.d2j.dex.writer.item.CodeItem;
import com.googlecode.d2j.dex.writer.item.DebugInfoItem;
import com.googlecode.d2j.dex.writer.item.DebugInfoItem.DNode;
import com.googlecode.d2j.dex.writer.item.CodeItem.EncodedCatchHandler;
import com.googlecode.d2j.dex.writer.item.CodeItem.EncodedCatchHandler.AddrPair;
import com.googlecode.d2j.dex.writer.item.CodeItem.TryItem;
import com.lee.dexdiff.utils.DiffUtils;
import com.lee.dexdiff.utils.Logger;

/**
 * Created by jianglee on 7/30/16.
 */
public class DiffCode implements Diff<CodeItem> {

    private static final String TAG = "DiffCode";
    public static final DiffCode sDefault = new DiffCode();

    @Override
    public boolean diff(CodeItem oldValue, CodeItem newValue) {
        if (null == oldValue || null == newValue) {
            throw new IllegalArgumentException("codeItem can not be null when diff.");
        }

        if (oldValue.registersSize != newValue.registersSize) {
            Logger.d(TAG, "registersSize is different -> " + newValue);
            return true;
        }

        if (oldValue.insSize != newValue.insSize) {
            Logger.d(TAG, "insSize is different -> " + newValue);
            return true;
        }

        if (oldValue.outsSize != newValue.outsSize) {
            Logger.d(TAG, "outsSize is different -> " + newValue);
            return true;
        }

        if (oldValue.insn_size != newValue.insn_size) {
            Logger.d(TAG, "insn_size is different -> " + newValue);
            return true;
        }

        // initial labels
        Map<Label, Integer> oldLabelPosMap = new HashMap<>();
        Map<Label, Integer> newLabelPosMap = new HashMap<>();
        // find labels in old code item
        int index = 0;
        if (null != oldValue._ops && oldValue._ops.size() > 0) {
            for (Insn insn : oldValue._ops) {
                if (insn instanceof Label) {
                    oldLabelPosMap.put((Label) insn, index++);
                }
            }
        }
        if (null != oldValue._tailOps && oldValue._tailOps.size() > 0) {
            for (Insn insn : oldValue._tailOps) {
                if (insn instanceof Label) {
                    oldLabelPosMap.put((Label) insn, index++);
                }
            }
        }
        // find labels in new code item
        index = 0;
        if (null != newValue._ops && newValue._ops.size() > 0) {
            for (Insn insn : newValue._ops) {
                if (insn instanceof Label) {
                    newLabelPosMap.put((Label) insn, index++);
                }
            }
        }
        if (null != newValue._tailOps && newValue._tailOps.size() > 0) {
            for (Insn insn : newValue._tailOps) {
                if (insn instanceof Label) {
                    newLabelPosMap.put((Label) insn, index++);
                }
            }
        }

        // try items
        if (!arrayTryItemEqual(oldValue._tryItems, newValue._tryItems, oldLabelPosMap, newLabelPosMap)) {
            Logger.d(TAG, "try items are different -> " + newValue);
            return true;
        }

        // operations
        if (!arrayInsnEqual(oldValue._ops, newValue._ops, oldLabelPosMap, newLabelPosMap)) {
            Logger.d(TAG, "operations are different -> " + newValue);
            return true;
        }

        // tail operations
        if (!arrayInsnEqual(oldValue._tailOps, newValue._tailOps, oldLabelPosMap, newLabelPosMap)) {
            Logger.d(TAG, "tail operations are different -> " + newValue);
            return true;
        }

        // debug info
        DebugInfoItem oldDebugItem = oldValue.debugInfo;
        DebugInfoItem newDebugItem = newValue.debugInfo;
        boolean isDebugDiff = false;
        if (null != oldDebugItem) {
            if (null != newDebugItem) {
                if (!Arrays.equals(oldDebugItem.parameterNames, newDebugItem.parameterNames)
                        || !DiffUtils.objectEqual(oldDebugItem.fileName, newDebugItem.fileName)
                        || oldDebugItem.firstLine != newDebugItem.firstLine || !arrayDNodeEqual(oldDebugItem.debugNodes,
                        newDebugItem.debugNodes, oldLabelPosMap, newLabelPosMap)) {
                    isDebugDiff = true;
                }
            } else {
                isDebugDiff = true;
            }
        } else if (null != newDebugItem) {
            isDebugDiff = true;
        }
        if (isDebugDiff) {
            Logger.d(TAG, "debug item is different -> " + newValue);
            return true;
        }

        return false;
    }

    private boolean arrayTryItemEqual(List<TryItem> oldInsns, List<TryItem> newInsns,
                                      Map<Label, Integer> oldLabelPosMap, Map<Label, Integer> newLabelPosMap) {
        if (null != oldInsns) {
            if (null != newInsns) {
                if (oldInsns.size() == newInsns.size()) {
                    TryItem oldItem = null;
                    TryItem newItem = null;
                    EncodedCatchHandler oldHandler = null;
                    EncodedCatchHandler newHandler = null;
                    for (int i = 0; i < oldInsns.size(); ++i) {
                        oldItem = oldInsns.get(i);
                        newItem = newInsns.get(i);
                        if (!labelEqual(oldItem.start, newItem.start, oldLabelPosMap, newLabelPosMap)
                                || !labelEqual(oldItem.end, newItem.end, oldLabelPosMap, newLabelPosMap)) {
                            return false;
                        }
                        oldHandler = oldItem.handler;
                        newHandler = newItem.handler;
                        if (!labelEqual(oldHandler.catchAll, newHandler.catchAll, oldLabelPosMap, newLabelPosMap)) {
                            return false;
                        }
                        List<AddrPair> oldAddPairs = oldHandler.addPairs;
                        List<AddrPair> newAddPairs = newHandler.addPairs;
                        if (null != oldAddPairs) {
                            if (null != newAddPairs) {
                                if (oldAddPairs.size() == newAddPairs.size()) {
                                    for (int j = 0; j < oldAddPairs.size(); ++j) {
                                        if (!DiffUtils.objectEqual(oldAddPairs.get(j).type, newAddPairs.get(j).type)
                                                || !labelEqual(oldAddPairs.get(j).addr, newAddPairs.get(j).addr,
                                                oldLabelPosMap, newLabelPosMap)) {
                                            return false;
                                        }
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (null != newAddPairs) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (null == newInsns) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean arrayInsnEqual(List<Insn> oldInsns, List<Insn> newInsns, Map<Label, Integer> oldLabelPosMap,
                                   Map<Label, Integer> newLabelPosMap) {
        if (null != oldInsns) {
            if (null != newInsns) {
                if (oldInsns.size() != newInsns.size()) {
                    return false;
                } else {
                    Insn oldInsn = null;
                    Insn newInsn = null;
                    for (int i = 0; i < oldInsns.size(); ++i) {
                        oldInsn = oldInsns.get(i);
                        newInsn = newInsns.get(i);
                        if (oldInsn instanceof Label) {
                            if (newInsn instanceof Label) {
                                if (!labelEqual((Label) oldInsn, (Label) newInsn, oldLabelPosMap, newLabelPosMap)) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof PreBuildInsn) {
                            if (newInsn instanceof PreBuildInsn) {
                                if (!Arrays.equals(((PreBuildInsn) oldInsn).data, ((PreBuildInsn) newInsn).data)) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof OP35c) {
                            if (newInsn instanceof OP35c) {
                                if (!DiffUtils.objectEqual(((OP35c) oldInsn).item, ((OP35c) newInsn).item)) {
                                    return false;
                                } else if (((OP35c) oldInsn).A != ((OP35c) newInsn).A
                                        || ((OP35c) oldInsn).C != ((OP35c) newInsn).C
                                        || ((OP35c) oldInsn).D != ((OP35c) newInsn).D
                                        || ((OP35c) oldInsn).E != ((OP35c) newInsn).E
                                        || ((OP35c) oldInsn).F != ((OP35c) newInsn).F
                                        || ((OP35c) oldInsn).G != ((OP35c) newInsn).G) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof OP3rc) {
                            if (newInsn instanceof OP3rc) {
                                if (!DiffUtils.objectEqual(((OP3rc) oldInsn).item, ((OP3rc) newInsn).item)) {
                                    return false;
                                } else if (((OP3rc) oldInsn).length != ((OP3rc) newInsn).length
                                        || ((OP3rc) oldInsn).start != ((OP3rc) newInsn).start) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof IndexedInsn) {
                            if (newInsn instanceof IndexedInsn) {
                                if (!DiffUtils.objectEqual(((IndexedInsn) oldInsn).idxItem,
                                        ((IndexedInsn) newInsn).idxItem)) {
                                    return false;
                                } else if (((IndexedInsn) oldInsn).a != ((IndexedInsn) newInsn).a
                                        || ((IndexedInsn) oldInsn).b != ((IndexedInsn) newInsn).b) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof JumpOp) {
                            if (newInsn instanceof JumpOp) {
                                if (!jumpOpEqual((JumpOp) oldInsn, (JumpOp) newInsn, oldLabelPosMap, newLabelPosMap)) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof PackedSwitch) {
                            if (newInsn instanceof PackedSwitch) {
                                // labels
                                if (null != ((PackedSwitch) oldInsn).labels) {
                                    if (null != ((PackedSwitch) newInsn).labels) {
                                        if (((PackedSwitch) oldInsn).labels.length != ((PackedSwitch) newInsn).labels.length) {
                                            return false;
                                        } else {
                                            for (int j = 0; j < ((PackedSwitch) oldInsn).labels.length; ++j) {
                                                if (!labelEqual(((PackedSwitch) oldInsn).labels[j],
                                                        ((PackedSwitch) newInsn).labels[j], oldLabelPosMap,
                                                        newLabelPosMap)) {
                                                    return false;
                                                }
                                            }
                                        }
                                    } else {
                                        return false;
                                    }
                                } else if (null != ((PackedSwitch) newInsn).labels) {
                                    return false;
                                }

                                // jumpOp
                                if (!jumpOpEqual(((PackedSwitch) oldInsn).jumpOp, ((PackedSwitch) newInsn).jumpOp,
                                        oldLabelPosMap, newLabelPosMap)) {
                                    return false;
                                }
                                // first_case
                                if (((PackedSwitch) oldInsn).first_case != ((PackedSwitch) newInsn).first_case) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else if (oldInsn instanceof SparseSwitch) {
                            if (newInsn instanceof SparseSwitch) {
                                // labels
                                if (null != ((SparseSwitch) oldInsn).labels) {
                                    if (null != ((SparseSwitch) newInsn).labels) {
                                        if (((SparseSwitch) oldInsn).labels.length != ((SparseSwitch) newInsn).labels.length) {
                                            return false;
                                        } else {
                                            for (int j = 0; j < ((SparseSwitch) oldInsn).labels.length; ++j) {
                                                if (!labelEqual(((SparseSwitch) oldInsn).labels[j],
                                                        ((SparseSwitch) newInsn).labels[j], oldLabelPosMap,
                                                        newLabelPosMap)) {
                                                    return false;
                                                }
                                            }
                                        }
                                    } else {
                                        return false;
                                    }
                                } else if (null != ((SparseSwitch) newInsn).labels) {
                                    return false;
                                }

                                // jumpOp
                                if (!jumpOpEqual(((SparseSwitch) oldInsn).jumpOp, ((SparseSwitch) newInsn).jumpOp,
                                        oldLabelPosMap, newLabelPosMap)) {
                                    return false;
                                }
                                // cases
                                if (!Arrays.equals(((SparseSwitch) oldInsn).cases, ((SparseSwitch) newInsn).cases)) {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            throw new IllegalArgumentException("unsupported operations");
                        }
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else {
            if (null == newInsns) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean arrayDNodeEqual(List<DNode> oldNodes, List<DNode> newNodes, Map<Label, Integer> oldLabelPosMap,
                                    Map<Label, Integer> newLabelPosMap) {
        if (null != oldNodes) {
            if (null != newNodes) {
                if (oldNodes.size() == newNodes.size()) {
                    DNode oldNode = null;
                    DNode newNode = null;
                    for (int i = 0; i < oldNodes.size(); ++i) {
                        oldNode = oldNodes.get(i);
                        newNode = newNodes.get(i);
                        if (null == oldNode || null == newNode) {
                            throw new IllegalArgumentException("DNode can not be null in debug info.");
                        }
                        if (oldNode.op != newNode.op || oldNode.reg != newNode.reg || oldNode.line != newNode.line
                                || !DiffUtils.objectEqual(oldNode.name, newNode.name)
                                || !DiffUtils.objectEqual(oldNode.type, newNode.type)
                                || !DiffUtils.objectEqual(oldNode.sig, newNode.sig)
                                || !labelEqual(oldNode.label, newNode.label, oldLabelPosMap, newLabelPosMap)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (null == newNodes) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean jumpOpEqual(JumpOp oldOp, JumpOp newOp, Map<Label, Integer> oldLabelPosMap,
                                Map<Label, Integer> newLabelPosMap) {
        if (!labelEqual(oldOp.label, newOp.label, oldLabelPosMap, newLabelPosMap) || oldOp.a != newOp.a
                || oldOp.b != newOp.b) {
            return false;
        }
        return true;
    }

    private boolean labelEqual(Label oldLabel, Label newLabel, Map<Label, Integer> oldLabelPosMap,
                               Map<Label, Integer> newLabelPosMap) {
        if (null != oldLabel) {
            if (null != newLabel) {
                if (oldLabelPosMap.containsKey(oldLabel) && newLabelPosMap.containsKey(newLabel)) {
                    if (!oldLabelPosMap.get(oldLabel).equals(newLabelPosMap.get(newLabel))) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    throw new IllegalStateException("No such label.");
                }
            } else {
                return false;
            }
        } else {
            if (null != newLabel) {
                return false;
            } else {
                return true;
            }
        }
    }
}
