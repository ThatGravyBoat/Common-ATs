package tech.thatgravyboat.commonats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AccessWidenerMapper {

    private final MappingDownloader.Output output;
    public final List<String> lines = new ArrayList<>();

    public AccessWidenerMapper(String accesswidener, String accesstransformer, MappingDownloader.Output output) {
        this.output = output;
        Set<String> accessTransformers = atLines(accesstransformer);
        String[] lines = accesswidener.split("\n");
        this.lines.add(lines[0]);
        for (String line : lines) {
            boolean accessible = line.startsWith("transitive-accessible");
            boolean extendable = line.startsWith("transitive-extendable");
            if (accessible || extendable) {
                String transformedLine = line.substring(22).trim();
                String[] parts = transformedLine.split("\t");
                if (!parts[0].equals("class") && extendable) {
                    continue;
                }

                String mappedLine = "public " + switch (parts[0]) {
                    case "field" -> mapField(parts);
                    case "method" -> mapMethod(parts);
                    case "class" -> mapClass(parts);
                    default -> throw new IllegalStateException("Unexpected value: " + parts[0]);
                };
                if (accessTransformers.contains(mappedLine)) {
                    this.lines.add(line);
                }
            }
        }
    }

    private static Set<String> atLines(String file) {
        Set<String> lines = new HashSet<>();
        for (String s : file.split("\n")) {
            // Skip comments
            String line = s.split("#")[0];
            if (line.trim().isEmpty()) {
                continue;
            }
            lines.add(line.trim());
        }
        return lines;
    }

    public List<String> lines() {
        return lines;
    }

    private String mapField(String[] parts) {
        String owner = parts[1];
        String name = parts[2];
        return mapClassName(owner) + " " + mapFieldName(name);
    }

    private String mapMethod(String[] parts) {
        String owner = parts[1];
        String name = parts[2];
        String desc = parts[3];
        StringBuilder mappedDesc = new StringBuilder();
        String siginiture = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
        String returnValue = desc.substring(desc.indexOf(')') + 1);
        mappedDesc.append("(");
        for (String s : siginiture.split(";")) {
            mappedDesc.append(mapSignature(s));
        }
        mappedDesc.append(")");
        mappedDesc.append(mapSignature(returnValue));
        return mapClassName(owner) + " " + mapMethodName(name) + mappedDesc;
    }

    private String mapClass(String[] parts) {
        return mapClassName(parts[1]);
    }

    private String mapClassName(String owner) {
        owner = owner.replace('/', '.');
        return this.output.classes().getOrDefault(owner, owner);
    }

    private String mapFieldName(String name) {
        return this.output.fields().getOrDefault(name, name);
    }

    private String mapMethodName(String method) {
        return this.output.methods().getOrDefault(method, method);
    }

    private String mapSignature(String signature) {
        StringBuilder current = new StringBuilder(signature);
        StringBuilder mappedSignature = new StringBuilder();
        while (current.length() > 0) {
            String type = current.substring(0, 1);
            current.delete(0, 1);
            if (type.equals("L")) {
                int end = current.indexOf(";");
                end = end == -1 ? current.length() : end;
                String className = current.substring(0, end);
                current.delete(0, className.length() + 1);
                mappedSignature.append("L");
                mappedSignature.append(mapClassName(className).replace('.', '/'));
                mappedSignature.append(";");
            } else if (type.equals("[")) {
                mappedSignature.append("[");
            } else {
                mappedSignature.append(type);
            }
        }
        return mappedSignature.toString();
    }

}
