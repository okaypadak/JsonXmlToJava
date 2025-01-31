package XmlXsdToJAXB4SOAP.component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import lombok.Data;

@Service
public class XMLToJAXB {

    private static List<ElementInfo> elementList = new ArrayList<>();

    public static void convert(File xmlFile, String fullOutputDir) throws Exception {// XML dosyanızın yolu
        parseXML(xmlFile);
        generateJAXBClass(fullOutputDir);
    }

    private static class ElementInfo {
        String name;
        String namespace;
        String parent;
        boolean isClass;
        String type;

        ElementInfo(String name, String namespace, String parent, boolean isClass, String type) {
            this.name = name;
            this.namespace = namespace;
            this.parent = parent;
            this.isClass = isClass;
            this.type = type;
        }
    }

    private static void parseXML(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        extractElements(doc.getDocumentElement(), null);
    }

    private static void extractElements(Element element, String parent) {
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
        }

        elementList.add(new ElementInfo(localName, namespaceURI, parent, isClass, type));

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                extractElements((Element) node, localName);
            }
        }
    }

    private static void generateJAXBClass(String outputPath) throws Exception {
        File file = new File(outputPath, "GeneratedJAXBClasses.java");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.generated;\n\n");
            writer.write("import javax.xml.bind.annotation.*;\n");
            writer.write("import lombok.Data;\n");
            writer.write("import java.math.BigDecimal;\n\n");

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
                writer.write("@XmlAccessorType(XmlAccessType.FIELD)\n");
                writer.write("@Getter\n");
                writer.write("@Setter\n");
                writer.write("public static class " + toClassName(element.name) + " {\n");
                generateClass(writer, element.name, classMap);
                writer.write("}\n");
            } else {
                if (element.namespace != null) {
                    writer.write("@XmlElement(name=\"" + element.name + "\", namespace=\"" + element.namespace + "\") ");
                } else {
                    writer.write("@XmlElement(name=\"" + element.name + "\") ");
                }
                writer.write("private " + element.type + " " + element.name.toLowerCase() + ";\n");
            }
        }
    }

    private static String toClassName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
