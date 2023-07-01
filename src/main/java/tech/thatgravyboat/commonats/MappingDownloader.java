package tech.thatgravyboat.commonats;

import com.google.gson.JsonObject;
import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.TsrgReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.*;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

public final class MappingDownloader {

    private static final HttpRequest MANIFEST_REQUEST = HttpRequest.newBuilder()
            .uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
            .GET()
            .build();

    private static JsonObject getManifest(String version) throws Exception {
        JsonObject manifest = Utils.getJson(MANIFEST_REQUEST);
        for (var entry : manifest.getAsJsonArray("versions")) {
            JsonObject obj = entry.getAsJsonObject();
            if (obj.get("id").getAsString().equals(version)) {
                return Utils.getJson(obj.get("url").getAsString());
            }
        }
        throw new Exception("Failed to find version");
    }

    private static Data getMappings(String version) throws Exception {
        JsonObject versionJson = getManifest(version);
        String serverMappings = Utils.getString(versionJson.getAsJsonObject("downloads")
                .getAsJsonObject("server_mappings")
                .get("url")
                .getAsString());
        String clientMappings = Utils.getString(versionJson.getAsJsonObject("downloads")
                .getAsJsonObject("client_mappings")
                .get("url")
                .getAsString());
        InputStream intermediary = Utils.getInputStream("https://maven.fabricmc.net/net/fabricmc/intermediary/" + version + "/intermediary-" + version + "-v2.jar");
        InputStream srg = Utils.getInputStream("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/" + version + "/mcp_config-" + version + ".zip");
        return new Data(serverMappings, clientMappings, intermediary, srg);
    }

    private static Output getFromData(Data data) throws Exception {
        final MemoryMappingTree tree = new MemoryMappingTree();
        try (
                StringReader serverMappings = new StringReader(data.serverMappings());
                StringReader clientMappings = new StringReader(data.clientMappings());
                ZipInputStream intermediary = new ZipInputStream(data.intermediary());
                ZipInputStream srg = new ZipInputStream(data.srg)
        ) {
            ProGuardReader.read(serverMappings, "mojang", "obf", tree);
            ProGuardReader.read(clientMappings, "mojang", "obf", tree);
            Tiny2Reader.read(readTinyMapping(intermediary), tree);
            TsrgReader.read(Utils.readFileInZip(srg, "config/joined.tsrg"), tree);

            final Map<String, String> classMapBuilder = new HashMap<>();
            final Map<String, String> methodMapBuilder = new HashMap<>();
            final Map<String, String> fieldMapBuilder = new HashMap<>();
            for (final MappingTree.ClassMapping clazz : tree.getClasses()) {
                final String icName = clazz.getName("intermediary");
                final String cName = clazz.getName("mojang");
                if (icName != null && cName != null) {
                    classMapBuilder.put(icName.replace("/", "."), cName.replace("/", "."));
                }
                for (final MappingTree.MethodMapping method : clazz.getMethods()) {
                    final String imName = method.getName("intermediary");
                    final String mName = method.getName("srg");
                    if (imName != null && mName != null) {
                        methodMapBuilder.put(imName, mName.intern());
                    }
                }
                for (final MappingTree.FieldMapping field : clazz.getFields()) {
                    final String ifName = field.getName("intermediary");
                    final String fName = field.getName("srg");
                    if (ifName != null && fName != null) {
                        fieldMapBuilder.put(ifName, fName.intern());
                    }
                }
            }
            return new Output(classMapBuilder, methodMapBuilder, fieldMapBuilder);
        }
    }

    private static StringReader readTinyMapping(ZipInputStream stream) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = Utils.readFileInZip(stream, "mappings/mappings.tiny");
        builder.append(reader.readLine().replace("official", "obf")).append("\n");
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return new StringReader(builder.toString());
    }

    public static Output download(String version) throws Exception {
        return getFromData(getMappings(version));
    }

    private record Data(String serverMappings, String clientMappings, InputStream intermediary, InputStream srg) {
    }

    public record Output(Map<String, String> classes, Map<String, String> methods, Map<String, String> fields) {
    }
}
