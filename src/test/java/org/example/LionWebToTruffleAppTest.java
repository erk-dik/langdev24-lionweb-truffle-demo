package org.example;

import org.f1re.demo.LionWebToTruffleApp;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

/**
 * Unit test for simple App.
 */
public class LionWebToTruffleAppTest {
    private final String language = "/simpleLanguage.json";
    private final String languageInstance = "/fib.json";
    private final InputStream languageInputStream = LionWebToTruffleAppTest.class.getResourceAsStream(language);
    private final InputStream languageInstanceInputStream = LionWebToTruffleAppTest.class.getResourceAsStream(languageInstance);
    @Test
    public void testApp() {
        LionWebToTruffleApp app = new LionWebToTruffleApp(languageInputStream, languageInstanceInputStream);
        Object executed = app.deserialize().convertToTruffleNodes().executeTruffleNodes();
        Assert.assertEquals(String.valueOf(55), executed.toString());

    }
}
