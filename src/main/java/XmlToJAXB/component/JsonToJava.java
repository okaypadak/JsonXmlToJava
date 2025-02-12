package XmlToJAXB.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

@Service
public class JsonToJava {

    private Map<String, List<ElementInfo>> classMap = new LinkedHashMap<>();

    public void convert(File file, String fullOutputDir) throws Exception {
        classMap.clear();
        parseJson(file);
        generateJavaClasses(fullOutputDir, file.getName());
    }

    private static class ElementInfo {
        String name;
        String type;
        boolean isClass;
        boolean isList;

        ElementInfo(String name, String type, boolean isClass, boolean isList) {
            this.name = name;
            this.type = type;
            this.isClass = isClass;
            this.isList = isList;
        }
    }

    private void parseJson(File file) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file);
        extractElements(rootNode, "Root");
    }

    private void extractElements(JsonNode node, String parentName) {

        if (node.isObject()) {
            List<ElementInfo> fields = classMap.computeIfAbsent(parentName, k -> new ArrayList<>());

            node.fieldNames().forEachRemaining(fieldName -> {
                JsonNode childNode = node.get(fieldName);
                boolean isClass = childNode.isObject();
                boolean isList = childNode.isArray();
                String type = determineType(fieldName, childNode);

                if (isList && !childNode.isEmpty()) {
                    extractElements(childNode.get(0), toClassName(fieldName));
                } else if (isClass) {
                    extractElements(childNode, toClassName(fieldName));
                }

                fields.add(new ElementInfo(fieldName, type, isClass, isList));

                if (isList && !classMap.containsKey(toClassName(fieldName))) {
                    classMap.put(toClassName(fieldName), new ArrayList<>());
                }
            });

        } else if (node.isArray() && !node.isEmpty()) {
            extractElements(node.get(0), parentName);
        }
    }

    private String determineType(String fieldName, JsonNode node) {
        if (node.isObject()) {
            return toClassName(fieldName);
        }
        if (node.isArray() && !node.isEmpty()) {
            JsonNode firstElement = node.get(0);
            if (firstElement.isObject()) {
                return "List<" + toClassName(fieldName) + ">";
            } else {
                return "List<" + determineType(fieldName, firstElement) + ">";
            }
        }
        if (node.isTextual()) return "String";
        if (node.isInt()) return "Integer";
        if (node.isBigDecimal() || node.isDouble()) return "BigDecimal";
        if (node.isBoolean()) return "Boolean";
        return "Object";
    }

    private void generateJavaClasses(String outputPath, String fileName) throws Exception {
        File file = new File(outputPath, fileName.replace(".json", ".java"));
        if (!file.exists()) file.createNewFile();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.generated;\n\n");
            writer.write("import java.util.List;\n");
            writer.write("import lombok.Getter;\nimport lombok.Setter;\n\n");

            generateNestedClass(writer, "Root", 0);
        }
    }

    private void generateNestedClass(FileWriter writer, String className, int indentLevel) throws Exception {
        String indent = "    ".repeat(indentLevel);
        writer.write(indent + "@Getter\n" + indent + "@Setter\n");
        writer.write(indent + "public static class " + className + " {\n");

        for (ElementInfo field : classMap.getOrDefault(className, new ArrayList<>())) {
            String fieldType = field.type;
            writer.write(indent + "    private " + fieldType + " " + field.name + ";\n");
        }

        for (ElementInfo field : classMap.getOrDefault(className, new ArrayList<>())) {
            if (field.isClass || field.isList) {
                generateNestedClass(writer, toClassName(field.name), indentLevel + 1);
            }
        }

        writer.write(indent + "}\n\n");
    }

    private static String toClassName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
