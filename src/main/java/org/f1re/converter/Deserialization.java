package org.f1re.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.lionweb.lioncore.java.language.Language;
import io.lionweb.lioncore.java.model.Node;
import io.lionweb.lioncore.java.serialization.JsonSerialization;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class Deserialization {
    private final JsonSerialization jsonSerialization;

    public Deserialization(InputStream inputStream){
        jsonSerialization = JsonSerialization.getStandardSerialization();
        jsonSerialization.enableDynamicNodes();
        JsonSerialization languageSerialization = JsonSerialization.getStandardSerialization();
        Language loadedLanguage = languageSerialization.loadLanguage(inputStream);
        jsonSerialization.registerLanguage(loadedLanguage);
    }

    public List<Node> getDeserializedLionWebNodes(InputStream inputStream){
        JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(inputStream));
        return jsonSerialization.deserializeToNodes(jsonElement);
    }
}
