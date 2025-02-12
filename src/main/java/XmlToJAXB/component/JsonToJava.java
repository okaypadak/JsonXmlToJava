package XmlToJAXB.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

@Service
public class JsonToJava {

    private List<ElementInfo> elementList = new ArrayList<>();
    private int idCounter = 1;
    private Set<String> processedElements = new HashSet<>();
    private Map<String, Integer> elementCount = new HashMap<>();

    public void convert(File file, String fullOutputDir) throws Exception {
        resetState();
        parseJson(file);
        generateJsonClass(fullOutputDir, file.getName());
    }

    private class ElementInfo {
        int id;
        String name;
        String parent;
        boolean isClass;
        boolean isRoot;
        String type;
        boolean isList;

        ElementInfo(int id, String name, String parent, boolean isClass, boolean isRoot, String type, boolean isList) {
            this.id = id;
            this.name = name;
            this.parent = parent;
            this.isClass = isClass;
            this.isRoot = isRoot;
            this.type = type;
            this.isList = isList;
        }
    }

    private void parseJson(File file) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file);
        extractElementsFromJson(rootNode, null, true);
    }

    private void extractElementsFromJson(JsonNode node, String parent, boolean isRoot) {

        if (node == null || node.isNull()) {
            return;
        }

        Iterator<String> fieldNames = node.fieldNames();
        if (!fieldNames.hasNext()) {
            return;
        }

        String localName = fieldNames.next();
        boolean isClass = false;
        boolean isList = false;
        String type = "String";

        if (parent != null && parent.equals(localName)) {
            localName += "Sub";
        }

        String elementKey = parent + ":" + localName;

        if (processedElements.contains(elementKey)) {
            return;
        }

        processedElements.add(elementKey);

        if (node.isObject()) {
            isClass = true;
        } else if (node.isArray() && node.size() > 1) {
            isList = true;
        } else {
            String content = node.asText().trim();
            if (content.matches("\\d+")) {
                type = "Integer";
            } else if (content.matches("\\d+\\.\\d+")) {
                type = "BigDecimal";
            }
        }

        isList = elementCount.getOrDefault(elementKey, 0) > 1;
        elementList.add(new ElementInfo(idCounter++, localName, parent, isClass, isRoot, type, isList));

        if (node.isObject()) {
            String finalLocalName = localName;
            node.fieldNames().forEachRemaining(fieldName -> {
                JsonNode childNode = node.get(fieldName);
                extractElementsFromJson(childNode, finalLocalName, false);
            });
        }
    }


    private void generateJsonClass(String outputPath, String name) throws Exception {

        File file = new File(outputPath, name.replace(".json", ".java"));

        if (!file.exists()) {
            file.createNewFile();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.generated;\n\n");
            writer.write("import java.util.List;\n");
            writer.write("import lombok.Getter;\n");
            writer.write("import lombok.Setter;\n\n");

            Map<String, List<ElementInfo>> classMap = new HashMap<>();
            for (ElementInfo element : elementList) {
                classMap.computeIfAbsent(element.parent, k -> new ArrayList<>()).add(element);
            }

            generateClass(name, writer, null, classMap, true); // İlk sınıfı oluştur
        }
    }

    private void generateClass(String name, FileWriter writer, String parent, Map<String, List<ElementInfo>> classMap, boolean isFirstClass) throws Exception {

        if (!classMap.containsKey(parent)) return;

        for (ElementInfo element : classMap.get(parent)) {
            if (element.isClass) {
                writer.write("public class " + (isFirstClass ? toClassName(name.replace(".json", "")) : toClassName(element.name)) + " {\n");

                for (ElementInfo child : classMap.getOrDefault(element.name, new ArrayList<>())) {
                    writer.write("    private " + (child.isList ? "List<" + toClassName(child.name) + ">" : (child.isClass ? toClassName(child.name) : child.type)) + " " + child.name + ";\n");
                }

                generateClass(name, writer, element.name, classMap, false);
                writer.write("}\n\n");
            }
        }
    }

    private static String toClassName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private void resetState() {
        elementList.clear();
        processedElements.clear();
        elementCount.clear();
        idCounter = 1;
    }
}
