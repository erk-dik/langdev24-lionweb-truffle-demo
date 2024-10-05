package org.f1re.demo;

import com.oracle.truffle.api.source.Source;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

public class App {
    private final String language = "/simpleLanguage.json";
    private final String languageInstance = "/fib.json";
    private final String fibSL = "/fib.sl";
    private final InputStream languageInputStream = App.class.getResourceAsStream(language);
    private final InputStream languageInstanceInputStream = App.class.getResourceAsStream(languageInstance);
    private final Reader fibSLInputStream = new InputStreamReader(
            Objects.requireNonNull(App.class.getResourceAsStream(fibSL)));

    public static void main(String[] args) {
        LionWebToTruffleApp lionWebToTruffleApp = new App().covertLwToTfNodes();

        final long startTime = System.nanoTime();
        Object executedTruffleNodes = lionWebToTruffleApp.executeTruffleNodes();
        final long endTime = System.nanoTime();

        final long timeTaken = endTime - startTime;
        System.out.printf("Execution time: %d us%n", timeTaken / 1000);

        Object executedSL = new App().executeSL();
        System.out.printf("Output matches: %s%n", executedSL.toString().equals(executedTruffleNodes.toString()));
    }

    private LionWebToTruffleApp covertLwToTfNodes() {
        LionWebToTruffleApp app = new LionWebToTruffleApp(languageInputStream, languageInstanceInputStream);
        return app
                .deserialize()
                .convertToTruffleNodes();
    }

    private Object executeSL() {
        Source source;
        try {
            source = Source.newBuilder("com/oracle/truffle/sl", fibSLInputStream, "").build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Engine engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        Context polyglot = Context.newBuilder("com/oracle/truffle/sl").engine(engine).build();
        polyglot.enter();
        return polyglot.eval("com/oracle/truffle/sl", source.getCharacters());
    }
}
