package tech.thatgravyboat.commonats;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class TransitiveAccessWideners {

    public static Result get(String mcVersion) throws Exception {
        String fabricApiVersion = null;
        for (String apiVersion : getApiVersions()) {
            if (isApiVersionvalidForMcVersion(apiVersion, mcVersion)) {
                fabricApiVersion = apiVersion;
                break;
            }
        }
        if (fabricApiVersion == null) {
            throw new IllegalStateException("Could not find a fabric api version for " + mcVersion);
        }
        try (ZipInputStream zip = new ZipInputStream(Utils.getInputStream("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/" + fabricApiVersion + "/fabric-api-" + fabricApiVersion + ".jar"))) {
            ZipInputStream transitiveJar = getTransitiveJar(zip);
            BufferedReader reader = Utils.readFileInZip(transitiveJar, "fabric-transitive-access-wideners-v1.accesswidener");
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return new Result(fabricApiVersion, builder.toString());
        }
    }

    private static ZipInputStream getTransitiveJar(ZipInputStream stream) throws Exception {
        ZipEntry entry;
        while ((entry = stream.getNextEntry()) != null) {
            if (entry.getName().startsWith("META-INF/jars/fabric-transitive-access-wideners-")) {
                return new ZipInputStream(stream);
            }
        }
        throw new IOException("Failed to find entry");
    }

    private static List<String> getApiVersions() throws Exception {
        InputStream text = Utils.getInputStream("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text);
        NodeList version = document.getElementsByTagName("version");
        List<String> versions = new ArrayList<>();
        for (int i = version.getLength() - 1; i >= 0; i--) {
            Node item = version.item(i);
            versions.add(item.getTextContent());
        }
        return versions;
    }

    private static final String[] VERSION_BRANCHES = {"1.14", "1.15", "1.16", "1.17", "1.18", "1.19", "1.20", "20w14infinite", "1.18_experimental"};

    /**
     * Taken from the TS source code for fabricmc.net <a href="https://github.com/FabricMC/fabricmc.net/blob/main/scripts/src/lib/Api.ts#L81">...</a>
     */
    private static boolean isApiVersionvalidForMcVersion(String apiVersion, String mcVersion) {
        if (mcVersion == null) return false;

        String branch = mcVersion;

        for (String v : VERSION_BRANCHES) {
            if (mcVersion.startsWith(v)) {
                branch = v;
            }
        }

        // Very dumb but idk of a better (easy) way.
        if (mcVersion.startsWith("22w13oneblockatatime")) branch = "22w13oneblockatatime";
        else if (mcVersion.startsWith("23w")) branch = "1.20";
        else if (mcVersion.startsWith("22w")) branch = "1.19.3";
        else if (mcVersion.startsWith("1.18.2")) branch = "1.18.2";
        else if (mcVersion.startsWith("1.19.1")) branch = "1.19.1";
        else if (mcVersion.startsWith("1.19.2")) branch = "1.19.2";
        else if (mcVersion.startsWith("1.19.3")) branch = "1.19.3";
        else if (mcVersion.startsWith("1.19.4")) branch = "1.19.4";
        else if (mcVersion.startsWith("1.20.1")) branch = "1.20.1";
        else if (mcVersion.startsWith("21w")) branch = "1.18";
        else if (mcVersion.startsWith("20w")) branch = "1.17";
        else if (mcVersion.startsWith("19w") || mcVersion.startsWith("18w")) branch = "1.14";

        return apiVersion.endsWith("-" + branch) || apiVersion.endsWith("+" + branch);
    }
}
