package tech.thatgravyboat.commonats;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Utils {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static HttpResponse.BodyHandler<JsonObject> ofJsonObject() {
        return (responseInfo) -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                string -> GSON.fromJson(string, JsonObject.class)
        );
    }

    public static String getString(HttpRequest request) throws Exception {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get string");
        }
        return response.body();
    }

    public static JsonObject getJson(HttpRequest request) throws Exception {
        HttpResponse<JsonObject> response = CLIENT.send(request, ofJsonObject());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get json");
        }
        return response.body();
    }

    public static InputStream getInputStream(HttpRequest request) throws Exception {
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get stream");
        }
        return response.body();
    }

    public static String getString(String url) throws Exception {
        return getString(HttpRequest.newBuilder().uri(URI.create(url)).GET().build());
    }

    public static JsonObject getJson(String url) throws Exception {
        return getJson(HttpRequest.newBuilder().uri(URI.create(url)).GET().build());
    }

    public static InputStream getInputStream(String url) throws Exception {
        return getInputStream(HttpRequest.newBuilder().uri(URI.create(url)).GET().build());
    }

    public static BufferedReader readFileInZip(ZipInputStream stream, String file) throws IOException {
        ZipEntry entry;
        while ((entry = stream.getNextEntry()) != null) {
            if (entry.getName().equals(file)) {
                return new BufferedReader(new InputStreamReader(stream));
            }
        }
        throw new IOException("Failed to find entry");
    }
}
