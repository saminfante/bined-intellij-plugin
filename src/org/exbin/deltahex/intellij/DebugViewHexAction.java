/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.deltahex.intellij;

import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.actions.XFetchValueActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.*;
import org.exbin.deltahex.intellij.debug.*;
import org.exbin.deltahex.intellij.panel.DebugViewPanel;
import org.exbin.deltahex.intellij.panel.ValuesPanel;
import org.exbin.utils.binary_data.BinaryData;
import org.exbin.utils.binary_data.ByteArrayData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Show debugger value in hexadecimal editor action.
 *
 * @author ExBin Project (http://exbin.org)
 * @version 0.1.6 2018/03/07
 */
public class DebugViewHexAction extends XFetchValueActionBase {

    @Override
    protected void handle(Project project, String value, XDebuggerTree tree) {
    }

    @NotNull
    @Override
    protected ValueCollector createCollector(@NotNull AnActionEvent e) {
        XValueNodeImpl node = getDataNode(e);
        return new ValueCollector(XDebuggerTree.getTree(e.getDataContext())) {
            DataDialog dialog = null;

            @Override
            public void handleInCollector(Project project, String value, XDebuggerTree tree) {
                String text = StringUtil.unquoteString(value);
                if (dialog == null) {
                    dialog = new DataDialog(project, text, node);
                    dialog.setTitle("View as Hexadecimal Data");
                    dialog.show();
                }
                dialog.setText(text);
            }
        };
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        if (getDataNode(event) != null) {
            event.getPresentation().setText("View Hex");
        }
    }

    private static XValueNodeImpl getDataNode(@NotNull AnActionEvent event) {
        List<XValueNodeImpl> selectedNodes = XDebuggerTreeActionBase.getSelectedNodes(event.getDataContext());
        if (selectedNodes.size() == 1) {
            XValueNodeImpl node = selectedNodes.get(0);
            XValue container = node.getValueContainer();
            if (container instanceof JavaValue && container.getModifier() != null) {
                ValueDescriptorImpl descriptor = ((JavaValue) container).getDescriptor();
                if (descriptor.isString() || descriptor.isArray() || descriptor.isPrimitive() || isBasicType(descriptor)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static boolean isBasicType(ValueDescriptorImpl descriptor) {
        final String type = descriptor.getDeclaredType();
        return CommonClassNames.JAVA_LANG_BYTE.equals(type)
        || CommonClassNames.JAVA_LANG_SHORT.equals(type)
        || CommonClassNames.JAVA_LANG_SHORT.equals(type)
        || CommonClassNames.JAVA_LANG_INTEGER.equals(type)
        || CommonClassNames.JAVA_LANG_LONG.equals(type)
        || CommonClassNames.JAVA_LANG_FLOAT.equals(type)
        || CommonClassNames.JAVA_LANG_DOUBLE.equals(type)
        || CommonClassNames.JAVA_LANG_CHARACTER.equals(type);
    }

    private static class DataDialog extends DialogWrapper {

        private final byte[] valuesCache = new byte[8];
        private final ByteBuffer byteBuffer = ByteBuffer.wrap(valuesCache);

        private final DebugViewPanel viewPanel;
        private final XValueNodeImpl myDataNode;

        private DataDialog(Project project, @Nullable String initialValue, @Nullable XValueNodeImpl dataNode) {
            super(project, false);
            myDataNode = dataNode;
            setModal(false);
            setCancelButtonText("Close");
            setOKButtonText("Set");
            getOKAction().setEnabled(false);
            setCrossClosesWindow(true);

            viewPanel = new DebugViewPanel();

            BinaryData data = null;

            if (myDataNode != null) {
                XValue container = myDataNode.getValueContainer();
                ValueDescriptorImpl descriptor = ((JavaValue) container).getDescriptor();
                if (descriptor.isPrimitive() || isBasicType(descriptor) || !descriptor.isNull()) {
                    if (descriptor.isArray()) {
                        data = processArrayData(descriptor);
                    } else {
                        data = processSimpleValue(descriptor);
                    }
                } else {
                    String rawValue = myDataNode.getRawValue();
                    if (rawValue != null) {
                        data = new ByteArrayData(rawValue.getBytes(Charset.defaultCharset()));
                    }
                }
            }

            if (data == null) {
                if (initialValue != null) {
                    data = new ByteArrayData(initialValue.getBytes(Charset.defaultCharset()));
                } else {
                    data = new ByteArrayData(new byte[0]);
                }
            }

            viewPanel.setData(data);

            init();
        }

        private BinaryData processArrayData(ValueDescriptorImpl descriptor) {
            final ArrayReference arrayRef = (ArrayReference) descriptor.getValue();
            final ArrayType arrayType = (ArrayType) descriptor.getType();
            if (arrayType != null) {
                final String componentType = arrayType.componentTypeName();
                switch (componentType) {
                    case CommonClassNames.JAVA_LANG_BYTE:
                    case "byte": {
                        return new DebugViewDataSource(new ByteArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_SHORT:
                    case "short": {
                        return new DebugViewDataSource(new ShortArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_INTEGER:
                    case "int": {
                        return new DebugViewDataSource(new IntegerArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_LONG:
                    case "long": {
                        return new DebugViewDataSource(new LongArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_FLOAT:
                    case "float": {
                        return new DebugViewDataSource(new FloatArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_DOUBLE:
                    case "double": {
                        return new DebugViewDataSource(new DoubleArrayPageProvider(arrayRef));
                    }
                    case CommonClassNames.JAVA_LANG_CHARACTER:
                    case "char": {
                        return new DebugViewDataSource(new CharArrayPageProvider(arrayRef));
                    }
                }
            }

            return null;
        }

        private BinaryData processSimpleValue(ValueDescriptorImpl descriptor) {
            final String type = descriptor.getDeclaredType();
            if (type == null)
                return null;

            switch (type) {
                case CommonClassNames.JAVA_LANG_BYTE:
                case "byte": {
                    ByteValue value = (ByteValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[1];
                    byteArray[0] = value.value();
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_SHORT:
                case "short": {
                    ShortValue valueRecord = (ShortValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[2];
                    short value = valueRecord.value();
                    byteArray[0] = (byte) (value >> 8);
                    byteArray[1] = (byte) (value & 0xff);
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_INTEGER:
                case "int": {
                    IntegerValue valueRecord = (IntegerValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[4];
                    int value = valueRecord.value();
                    byteArray[0] = (byte) (value >> 24);
                    byteArray[1] = (byte) ((value >> 16) & 0xff);
                    byteArray[2] = (byte) ((value >> 8) & 0xff);
                    byteArray[3] = (byte) (value & 0xff);
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_LONG:
                case "long": {
                    LongValue valueRecord = (LongValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[8];
                    long value = valueRecord.value();
                    BigInteger bigInteger = BigInteger.valueOf(value);
                    for (int bit = 0; bit < 7; bit++) {
                        BigInteger nextByte = bigInteger.and(ValuesPanel.BIG_INTEGER_BYTE_MASK);
                        byteArray[7 - bit] = nextByte.byteValue();
                        bigInteger = bigInteger.shiftRight(8);
                    }
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_FLOAT:
                case "float": {
                    FloatValue valueRecord = (FloatValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[4];
                    float value = valueRecord.value();
                    byteBuffer.rewind();
                    byteBuffer.putFloat(value);
                    System.arraycopy(valuesCache, 0, byteArray, 0, 4);
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_DOUBLE:
                case "double": {
                    DoubleValue valueRecord = (DoubleValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[8];
                    double value = valueRecord.value();
                    byteBuffer.rewind();
                    byteBuffer.putDouble(value);
                    System.arraycopy(valuesCache, 0, byteArray, 0, 8);
                    return new ByteArrayData(byteArray);
                }
                case CommonClassNames.JAVA_LANG_CHARACTER:
                case "char": {
                    CharValue valueRecord = (CharValue) getPrimitiveValue(descriptor);
                    byte[] byteArray = new byte[2];
                    char value = valueRecord.value();
                    byteBuffer.rewind();
                    byteBuffer.putChar(value);
                    System.arraycopy(valuesCache, 0, byteArray, 0, 2);
                    return new ByteArrayData(byteArray);
                }
            }

            return null;
        }

        private Value getPrimitiveValue(ValueDescriptorImpl descriptor) {
            if (descriptor.isPrimitive())
                return descriptor.getValue();

            Field field = ((ObjectReference) descriptor.getValue()).referenceType().fieldByName("value");
            Value value = ((ObjectReference) descriptor.getValue()).getValue(field);
            return value;
        }

        public void setText(String text) {
            // TODO
        }

        @Override
        protected void doOKAction() {
//            if (myDataNode != null) {
//                DebuggerUIUtil.setTreeNodeValue(myDataNode,
//                        StringUtil.wrapWithDoubleQuote(DebuggerUtils.translateStringValue(codeArea.getData())),
//                        errorMessage -> Messages.showErrorDialog(myDataNode.getTree(), errorMessage));
//            }
            super.doOKAction();
        }

        @Override
        @NotNull
        protected Action[] createActions() {
            return myDataNode != null ? new Action[]{getOKAction(), getCancelAction()} : new Action[]{getCancelAction()};
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return viewPanel;
        }

        @Override
        protected String getDimensionServiceKey() {
            return "#org.exbin.deltahex.intellij.DebugViewHexAction";
        }

        @Override
        protected JComponent createCenterPanel() {
            BorderLayoutPanel panel = JBUI.Panels.simplePanel(viewPanel);
            panel.setPreferredSize(JBUI.size(600, 400));
            return panel;
        }
    }
}
