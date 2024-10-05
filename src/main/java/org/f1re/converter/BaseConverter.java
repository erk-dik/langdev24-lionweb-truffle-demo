package org.f1re.converter;

import io.lionweb.lioncore.java.model.Node;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseConverter {
    protected final List<Node> lwNodes;
    protected final List<com.oracle.truffle.api.nodes.Node> tfNodes;

    protected BaseConverter(List<Node> lwNodes) {
        this.lwNodes = lwNodes;
        this.tfNodes = new ArrayList<>();
    }

    public List<com.oracle.truffle.api.nodes.Node> convert() {
        lwNodes.forEach((node) -> tfNodes.add(convert(node)));
        return tfNodes;
    }

    protected abstract <T extends com.oracle.truffle.api.nodes.Node> T convert(io.lionweb.lioncore.java.model.Node lwNode);
}
