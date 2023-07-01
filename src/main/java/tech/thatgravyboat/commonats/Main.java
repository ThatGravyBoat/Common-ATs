package tech.thatgravyboat.commonats;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java -jar commonats.jar <mcversion>");
        }
        String version = args[0];

        Result aws = TransitiveAccessWideners.get(version);
        Result ats = AccessTransformers.get(version);
        MappingDownloader.Output output = MappingDownloader.download(version);
        AccessWidenerMapper mapper = new AccessWidenerMapper(aws.content(), ats.content(), output);
        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);
        Files.write(outputDir.resolve("common.accesswidener"), String.join("\n", mapper.lines()).getBytes());
    }
}
