package io.mango.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Regression for Issue #1270.
 *
 * <p>Maven 3 has no Plexus converter for {@code java.nio.file.Path}, so any plugin parameter declared
 * with that interface fails configuration parsing before the goal runs:
 * {@code Cannot create instance of interface java.nio.file.Path}. Plugin parameters must use
 * {@code java.io.File} and convert to {@code Path} inside the plugin.
 */
class PluginDescriptorPathParameterTest {

    private static final String PATH_TYPE = "java.nio.file.Path";

    @Test
    void everyDescriptorParameterUsesPlexusBindableType() throws Exception {
        Document descriptor;
        try (InputStream stream =
                PluginDescriptorPathParameterTest.class
                        .getClassLoader()
                        .getResourceAsStream("META-INF/maven/plugin.xml")) {
            assertTrue(stream != null, "missing generated plugin descriptor");
            descriptor = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        }

        List<String> pathParameters = new ArrayList<>();
        NodeList mojos = descriptor.getElementsByTagName("mojo");
        for (int index = 0; index < mojos.getLength(); index++) {
            Element mojo = (Element) mojos.item(index);
            String goal = text(mojo, "goal");
            for (Element parameter : parameters(mojo)) {
                if (PATH_TYPE.equals(text(parameter, "type"))) {
                    pathParameters.add(goal + ":" + text(parameter, "name"));
                }
            }
        }

        assertTrue(
                pathParameters.isEmpty(),
                "parameters must be java.io.File, not " + PATH_TYPE + ": " + pathParameters);
    }

    private static List<Element> parameters(Element mojo) {
        List<Element> result = new ArrayList<>();
        NodeList list = mojo.getElementsByTagName("parameter");
        for (int index = 0; index < list.getLength(); index++) {
            result.add((Element) list.item(index));
        }
        return result;
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
