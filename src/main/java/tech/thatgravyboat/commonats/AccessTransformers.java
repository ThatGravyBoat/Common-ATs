package tech.thatgravyboat.commonats;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

public final class AccessTransformers {

    public static Result get(String mcVersion) throws Exception {
        String forgeApiVersion = null;
        for (String apiVersion : getApiVersions()) {
            if (apiVersion.startsWith(mcVersion + "-")) {
                forgeApiVersion = apiVersion;
                break;
            }
        }
        if (forgeApiVersion == null) {
            throw new IllegalStateException("Could not find a forge api version for " + mcVersion);
        }
        InputStream jar = Utils.getInputStream("https://maven.minecraftforge.net/net/minecraftforge/forge/" + forgeApiVersion + "/forge-" + forgeApiVersion + "-universal.jar");
        try (ZipInputStream zip = new ZipInputStream(jar)) {
            BufferedReader reader = Utils.readFileInZip(zip, "META-INF/accesstransformer.cfg");
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return new Result(forgeApiVersion, builder.toString());
        }
    }

    private static List<String> getApiVersions() throws Exception {
        InputStream text = Utils.getInputStream("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text);
        NodeList version = document.getElementsByTagName("version");
        List<String> versions = new ArrayList<>();
        for (int i = 0; i < version.getLength(); i++) {
            Node item = version.item(i);
            versions.add(item.getTextContent());
        }
        return versions;
    }
}
