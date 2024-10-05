package org.f1re.demo;

import com.oracle.truffle.sl.nodes.SLRootNode;
import io.lionweb.lioncore.java.model.Node;
import org.f1re.converter.Deserialization;
import org.f1re.converter.LionWebToTruffleConverter;

import java.io.InputStream;
import java.util.List;

public class LionWebToTruffleApp {
    private final InputStream language;
    private final InputStream languageInstance;
    private List<Node> lwNodes;
    private List<com.oracle.truffle.api.nodes.Node> tfNodes;

    public LionWebToTruffleApp(InputStream language, InputStream languageInstance) {
        this.language = language;
        this.languageInstance = languageInstance;
    }

    public Object executeTruffleNodes() {
        SLRootNode rootNode = (SLRootNode) tfNodes.getFirst();
        return rootNode.getCallTarget().call();
    }

    public LionWebToTruffleApp convertToTruffleNodes() {
        tfNodes = new LionWebToTruffleConverter(lwNodes).convert();
        return this;
    }

    public LionWebToTruffleApp deserialize() {
        Deserialization lionwebDeserialization = new Deserialization(language);
        lwNodes = lionwebDeserialization.getDeserializedLionWebNodes(languageInstance);
        return this;
    }
}
