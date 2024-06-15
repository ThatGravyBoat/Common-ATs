package tech.thatgravyboat.commonats;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

public final class AccessTransformers {

    private static Pattern toNeoVersionStart(String mcVersion) {
        var split = mcVersion.split("\\.");
        if (split.length == 2) {
            return Pattern.compile(split[1] + "\\.0\\.\\d+(?:-beta)?");
        } else {
            return Pattern.compile(split[1] + "\\." + split[2] + "\\.\\d+(?:-beta)?");
        }
    }

    public static Result get(String mcVersion) throws Exception {
        Pattern neoVersionPattern = toNeoVersionStart(mcVersion);
        String neoApiVersion = null;
        for (String apiVersion : getApiVersions()) {
            if (neoVersionPattern.matcher(apiVersion).matches()) {
                neoApiVersion = apiVersion;
                break;
            }
        }
        if (neoApiVersion == null) {
            throw new IllegalStateException("Could not find a forge api version for " + mcVersion);
        }
        InputStream jar = Utils.getInputStream("https://maven.neoforged.net/net/neoforged/neoforge/" + neoApiVersion + "/neoforge-" + neoApiVersion + "-universal.jar");
        try (ZipInputStream zip = new ZipInputStream(jar)) {
            BufferedReader reader = Utils.readFileInZip(zip, "META-INF/accesstransformer.cfg");
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return new Result(neoApiVersion, builder.toString());
        }
    }

    private static List<String> getApiVersions() throws Exception {
        InputStream text = Utils.getInputStream("https://maven.neoforged.net/net/neoforged/neoforge/maven-metadata.xml");
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
