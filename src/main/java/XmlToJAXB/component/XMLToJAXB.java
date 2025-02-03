package XmlToJAXB.component;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class XMLToJAXB {

    private static List<ElementInfo> elementList = new ArrayList<>();

    public static void convert(File xmlFile, String fullOutputDir) throws Exception {
        parseXML(xmlFile);
        generateJAXBClass(fullOutputDir);
    }

    private static class ElementInfo {
        String name;
        String namespace;
        String parent;
        boolean isClass;
        boolean isRoot;
        String type;

        ElementInfo(String name, String namespace, String parent, boolean isClass, boolean isRoot, String type) {
            this.name = name;
            this.namespace = namespace;
            this.parent = parent;
            this.isClass = isClass;
            this.isRoot = isRoot;
            this.type = type;
        }
    }

    private static void parseXML(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        extractElements(doc.getDocumentElement(), null, true);
    }

    private static void extractElements(Element element, String parent, boolean isRoot) {
        String namespaceURI = element.getNamespaceURI();
        String localName = element.getLocalName();
        boolean isClass = false;
        String type = "String";

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                isClass = true;
                break;
            }
        }

        if (!isClass) {
            String content = element.getTextContent().trim();
            if (content.matches("\\d+")) {
                type = "Long";
            } else if (content.matches("\\d+\\.\\d+")) {
                type = "BigDecimal";
            }
        } else {
            type = toClassName(localName);
        }

        elementList.add(new ElementInfo(localName, namespaceURI, parent, isClass, isRoot, type));

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                extractElements((Element) node, localName, false);
            }
        }
    }

    private static void generateJAXBClass(String outputPath) throws Exception {
        File file = new File(outputPath, "GeneratedJAXBClasses.java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.generated;\n\n");
            writer.write("import jakarta.xml.bind.annotation.*;\n");
            writer.write("import lombok.Getter;\nimport lombok.Setter;\n\n");

            Map<String, List<ElementInfo>> classMap = new HashMap<>();
            for (ElementInfo element : elementList) {
                classMap.computeIfAbsent(element.parent, k -> new ArrayList<>()).add(element);
            }

            generateClass(writer, null, classMap);
        }
    }

    private static void generateClass(FileWriter writer, String parent, Map<String, List<ElementInfo>> classMap) throws Exception {
        if (!classMap.containsKey(parent)) return;

        for (ElementInfo element : classMap.get(parent)) {
            if (element.isClass) {
                if (element.isRoot) {
                    writer.write("@XmlRootElement(name=\"" + element.name + "\"" + (element.namespace != null ? ", namespace=\"" + element.namespace + "\"" : "") + ")\n");
                    writer.write("@XmlType(name = \"" + "\"" + ", propOrder = {" + getPropOrder(element.name, classMap) + "})\n");
                    writer.write("public class " + toClassName(element.name) + " {\n");
                } else {
                    writer.write("@XmlType(name = \"" + element.name + "\"" + (element.namespace != null ? ", namespace=\"" + element.namespace + "\"" : "") + ", propOrder = {" + getPropOrder(element.name, classMap) + "})\n");
                    writer.write("public static class " + toClassName(element.name) + " {\n");
                }

                writer.write("@XmlAccessorType(XmlAccessType.FIELD)\n");
                writer.write("@Getter\n@Setter\n");

                for (ElementInfo child : classMap.getOrDefault(element.name, new ArrayList<>())) {
                    writer.write("    @XmlElement(name=\"" + child.name + "\"" + (child.namespace != null ? ", namespace=\"" + child.namespace + "\"" : "") + ")\n");
                    writer.write("    private " + (child.isClass ? toClassName(child.name) : child.type) + " " + child.name + ";\n");
                }

                generateClass(writer, element.name, classMap);
                writer.write("}\n\n");
            }
        }
    }

    private static String getPropOrder(String className, Map<String, List<ElementInfo>> classMap) {
        return classMap.getOrDefault(className, new ArrayList<>()).stream()
                .map(e -> "\"" + e.name + "\"")
                .collect(Collectors.joining(", "));
    }

    private static String toClassName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
