package org.f1re.converter;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.sl.nodes.SLExpressionNode;
import com.oracle.truffle.sl.nodes.SLRootNode;
import com.oracle.truffle.sl.nodes.SLStatementNode;
import com.oracle.truffle.sl.nodes.controlflow.*;
import com.oracle.truffle.sl.nodes.expression.*;
import com.oracle.truffle.sl.nodes.local.SLReadArgumentNode;
import com.oracle.truffle.sl.nodes.local.SLReadLocalVariableNodeGen;
import com.oracle.truffle.sl.nodes.local.SLWriteLocalVariableNodeGen;
import com.oracle.truffle.sl.runtime.SLStrings;
import io.lionweb.lioncore.java.model.ReferenceValue;

import java.util.*;

public class LionWebToTruffleConverter extends BaseConverter {
    static class LexicalScope {
        protected final LexicalScope outer;
        protected final Map<TruffleString, Integer> locals;

        LexicalScope(LexicalScope outer) {
            this.outer = outer;
            this.locals = new HashMap<>();
        }

        public Integer find(TruffleString name) {
            Integer result = locals.get(name);
            if (result != null) {
                return result;
            } else if (outer != null) {
                return outer.find(name);
            } else {
                return null;
            }
        }
    }

    /* State while parsing a block. */
    private LexicalScope lexicalScope;
    private final FrameDescriptor.Builder frameDescriptorBuilder;
    private final Map<TruffleString, RootCallTarget> allFunctions;
    private int parameterCount;

    public LionWebToTruffleConverter(List<io.lionweb.lioncore.java.model.Node> lwNodes) {
        super(lwNodes);
        this.frameDescriptorBuilder = FrameDescriptor.newBuilder();
        this.allFunctions = new HashMap<>();
        startBlock();
    }

    private void startBlock() {
        lexicalScope = new LexicalScope(lexicalScope);
    }

    private void flattenBlocks(Iterable<? extends SLStatementNode> bodyNodes, List<SLStatementNode> flattenedNodes) {
        for (SLStatementNode n : bodyNodes) {
            if (n instanceof SLBlockNode) {
                flattenBlocks(((SLBlockNode) n).getStatements(), flattenedNodes);
            } else {
                flattenedNodes.add(n);
            }
        }
    }

    public SLExpressionNode createCall(SLExpressionNode functionNode, List<SLExpressionNode> parameterNodes) {
        if (functionNode == null || containsNull(parameterNodes)) {
            return null;
        }
        return new SLInvokeNode(
                functionNode, parameterNodes.toArray(new SLExpressionNode[parameterNodes.size()]));
    }

    public SLExpressionNode createAssignment(SLExpressionNode nameNode, SLExpressionNode valueNode) {
        return createAssignment(nameNode, valueNode, null);
    }

    private SLExpressionNode createAssignment(SLExpressionNode nameNode, SLExpressionNode valueNode, Integer argumentIndex) {
        if (nameNode == null || valueNode == null) {
            return null;
        }

        TruffleString name = ((SLStringLiteralNode) nameNode).executeGeneric(null);

        Integer frameSlot = lexicalScope.find(name);
        boolean newVariable = false;
        if (frameSlot == null) {
            frameSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Illegal, name, argumentIndex);
            lexicalScope.locals.put(name, frameSlot);
            newVariable = true;
        }
        final SLExpressionNode result = SLWriteLocalVariableNodeGen.create(valueNode, frameSlot, nameNode, newVariable);

        if (valueNode.hasSource()) {
            final int start = nameNode.getSourceCharIndex();
            final int length = valueNode.getSourceEndIndex() - start;
            result.setSourceSection(start, length);
        }
        if (argumentIndex == null) {
            result.addExpressionTag();
        }

        return result;
    }

    public SLExpressionNode createRead(SLExpressionNode nameNode) {
        if (nameNode == null) {
            return null;
        }

        TruffleString name = ((SLStringLiteralNode) nameNode).executeGeneric(null);
        final SLExpressionNode result;
        final Integer frameSlot = lexicalScope.find(name);
        if (frameSlot != null) {
            /* Read of a local variable. */
            result = SLReadLocalVariableNodeGen.create(frameSlot);
        } else {
            /* Read of a global name. In our language, the only global names are functions. */
            result = new SLFunctionLiteralNode(name);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends Node> T convert(io.lionweb.lioncore.java.model.Node lwNode) {
        String classifierName = lwNode.getClassifier().getName();

        switch (Objects.requireNonNull(classifierName)) {
            case "MainFunction":
                //startBlock();
                List<SLStatementNode> methodNodes = new ArrayList<>();
                String methodName = String.valueOf(lwNode.getPropertyValue(lwNode.getClassifier().getPropertyByName("name")));
                //add arguments
                List<? extends io.lionweb.lioncore.java.model.Node> arguments = lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("arguments"));
                if (!arguments.isEmpty())
                    methodNodes.add(convert(arguments.getFirst()));
                // add body
                methodNodes.add(convert(lwNode.getChildren(lwNode.getClassifier().getContainmentByName("body")).getFirst()));
                final SLStatementNode methodBlock = new SLBlockNode(methodNodes.toArray(new SLStatementNode[0]));
                final SLFunctionBodyNode functionBodyNode = new SLFunctionBodyNode(methodBlock);
                SLRootNode slRootNode = new SLRootNode(
                        null, frameDescriptorBuilder.build(), functionBodyNode, null, SLStrings.fromJavaString(methodName));
                allFunctions.put(SLStrings.fromJavaString(methodName), slRootNode.getCallTarget());
                parameterCount = 0;
                // frameDescriptorBuilder = null;
                //lexicalScope = null;
                return (T) slRootNode;

            case "FunctionArgument":
                final SLReadArgumentNode readArg = new SLReadArgumentNode(parameterCount);
                String argumentName = String.valueOf(lwNode.getPropertyValue(lwNode.getClassifier().getPropertyByName("name")));
                SLExpressionNode argumentNameNode = new SLStringLiteralNode(SLStrings.fromJavaString(argumentName));
                SLExpressionNode assignment = createAssignment(argumentNameNode, readArg, parameterCount);
                parameterCount++;
                return (T) assignment;

            case "Function":
                break;

            case "FunctionCallExpression":
                List<SLExpressionNode> argNodes = new ArrayList<>();
                List<? extends io.lionweb.lioncore.java.model.Node> funCallArguments =
                        lwNode.getChildren(lwNode.getClassifier().getContainmentByName("arguments"));
                for (io.lionweb.lioncore.java.model.Node arg : funCallArguments) {
                    argNodes.add(convert(arg));
                }
                SLExpressionNode[] argumentNodes = argNodes.toArray(new SLExpressionNode[0]);
                List<ReferenceValue> targetFunctionReferences = lwNode.getReferenceValues(Objects.requireNonNull(
                        lwNode.getClassifier().getReferenceByName("target")));
                String resolveTargetInfo = null;
                for (ReferenceValue ref : targetFunctionReferences) {
                    resolveTargetInfo = ref.getResolveInfo();
                }
                SLExpressionNode slFunctionLiteralNode =
                        new SLFunctionLiteralNode(SLStrings.fromJavaString(resolveTargetInfo));

                return (T) new SLInvokeNode(slFunctionLiteralNode, argumentNodes);

            case "Block":
                List<SLStatementNode> bodyNodes = new ArrayList<>();
                List<? extends io.lionweb.lioncore.java.model.Node> statements = lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("statements"));
                for (io.lionweb.lioncore.java.model.Node statement : statements) {
                    bodyNodes.add(convert(statement));
                }
                List<SLStatementNode> flattenedNodes = new ArrayList<>(bodyNodes.size());
                flattenBlocks(bodyNodes, flattenedNodes);
                return (T) new SLBlockNode(flattenedNodes.toArray(new SLStatementNode[0]));
            //return (T) new SLBlockNode(bodyNodes.toArray(new SLStatementNode[0]));

            case "WriteLocalVariableStatement":
                SLExpressionNode valueNode = convert(
                        lwNode.getChildren(lwNode.getClassifier().getContainmentByName("value")).getFirst());
                String variableName = String.valueOf(
                        lwNode.getPropertyValue(lwNode.getClassifier().getPropertyByName("name")));
                SLExpressionNode nameNode = new SLStringLiteralNode(SLStrings.fromJavaString(variableName));
                return (T) createAssignment(nameNode, valueNode);

            case "ReadLocalVariableExpression":
                List<ReferenceValue> referenceValues = lwNode.getReferenceValues(
                        Objects.requireNonNull(lwNode.getClassifier().getReferenceByName("variable")));
                String resolveInfo = null;
                for (ReferenceValue ref : referenceValues) {
                    resolveInfo = ref.getResolveInfo();
                }
                SLExpressionNode nameTargetNode = new SLStringLiteralNode(SLStrings.fromJavaString(resolveInfo));
                return (T) createRead(nameTargetNode);

            case "IfStatement":
                SLExpressionNode ifConditionNode = convert(lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("condition")).getFirst());
                SLStatementNode thenPartNode = convert(lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("then")).getFirst());
                List<? extends io.lionweb.lioncore.java.model.Node> elseNode = lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("else"));

                if (!elseNode.isEmpty()) {
                    SLStatementNode elsePartNode = convert(elseNode.getFirst());
                    return (T) new SLIfNode(ifConditionNode, thenPartNode, elsePartNode);
                } else
                    return (T) new SLIfNode(ifConditionNode, thenPartNode, null);

            case "WhileStatement":
                SLExpressionNode whileConditionNode = convert(lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("condition")).getFirst());
                SLStatementNode bodyNode = convert(lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("body")).getFirst());
                return (T) new SLWhileNode(whileConditionNode, bodyNode);

            case "AddExpression":
                return (T) SLAddNodeGen.create(
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("lhs")).getFirst()),
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("rhs")).getFirst()));

            case "SubExpression":
                return (T) SLSubNodeGen.create(
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("lhs")).getFirst()),
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("rhs")).getFirst()));

            case "LessThanExpression":
                return (T) SLLessThanNodeGen.create(
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("lhs")).getFirst()),
                        convert(lwNode.getChildren(
                                lwNode.getClassifier().getContainmentByName("rhs")).getFirst()));

            case "LongLiteral":
                long value = Long.parseLong(
                        lwNode.getPropertyValue(lwNode.getClassifier().getPropertyByName("value")).toString());
                return (T) new SLLongLiteralNode(value);

            case "StringLiteral":
                TruffleString truffleString = SLStrings.fromJavaString(Objects.requireNonNull(
                        lwNode.getClassifier().getPropertyByName("value")).toString());
                return (T) new SLStringLiteralNode(truffleString);

            case "ReturnStatement":
                SLExpressionNode slExpressionNode = convert(lwNode.getChildren(
                        lwNode.getClassifier().getContainmentByName("value")).getFirst());
                return (T) new SLReturnNode(slExpressionNode);
            default:
        }

        throw new IllegalStateException("unknown classifier: %s".formatted(classifierName));
    }

    public SLExpressionNode createStringLiteral(TruffleString literalToken) {
        return new SLStringLiteralNode(literalToken);
    }

    private static boolean containsNull(List<?> list) {
        for (Object e : list) {
            if (e == null) {
                return true;
            }
        }
        return false;
    }
}

