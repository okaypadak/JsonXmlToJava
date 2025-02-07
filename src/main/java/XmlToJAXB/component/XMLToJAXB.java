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

    private List<ElementInfo> elementList = new ArrayList<>();
    private int idCounter = 1;
    private Set<String> processedElements = new HashSet<>();
    private Map<String, Integer> elementCount = new HashMap<>();

    public void convert(File xmlFile, String fullOutputDir) throws Exception {
        resetState();
        parseXML(xmlFile);
        generateJAXBClass(fullOutputDir, xmlFile.getName());
    }

    private class ElementInfo {
        int id;
        String name;
        String namespace;
        String parent;
        boolean isClass;
        boolean isRoot;
        String type;
        boolean isList;

        ElementInfo(int id, String name, String namespace, String parent, boolean isClass, boolean isRoot, String type, boolean isList) {
            this.id = id;
            this.name = name;
            this.namespace = namespace;
            this.parent = parent;
            this.isClass = isClass;
            this.isRoot = isRoot;
            this.type = type;
            this.isList = isList;
        }
    }

    private void parseXML(File file) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        extractElements(doc.getDocumentElement(), null, true);
    }

    private void extractElements(Element element, String parent, boolean isRoot) {

        String namespaceURI = element.getNamespaceURI();
        String localName = element.getLocalName();
        boolean isClass = false;
        String type = "String";
        boolean isList = false;

        if (parent != null && parent.equals(localName)) {
            localName += "Sub";
        }

        String elementKey = parent + ":" + localName;

        if (processedElements.contains(elementKey)) {
            return;
        }

        processedElements.add(elementKey);

        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                String childName = ((Element) node).getLocalName();
                String childKey = localName + ":" + childName;
                elementCount.put(childKey, elementCount.getOrDefault(childKey, 0) + 1);
                isClass = true;
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

        isList = elementCount.getOrDefault(elementKey, 0) > 1;
        elementList.add(new ElementInfo(idCounter++, localName, namespaceURI, parent, isClass, isRoot, type, isList));

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                extractElements((Element) node, localName, false);
            }
        }
    }

    private void generateJAXBClass(String outputPath, String name) throws Exception {
        File file = new File(outputPath, name.replace(".xml",".java"));

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("package com.generated;\n\n");
            writer.write("import jakarta.xml.bind.annotation.*;\n");
            writer.write("import lombok.Getter;\nimport lombok.Setter;\nimport java.util.List;\n\n");

            Map<String, List<ElementInfo>> classMap = new HashMap<>();
            for (ElementInfo element : elementList) {
                classMap.computeIfAbsent(element.parent, k -> new ArrayList<>()).add(element);
            }

            generateClass(writer, null, classMap, true);
        }
    }

    private void generateClass(FileWriter writer, String parent, Map<String, List<ElementInfo>> classMap, boolean isFirstClass) throws Exception {
        if (!classMap.containsKey(parent)) return;

        for (ElementInfo element : classMap.get(parent)) {
            if (element.isClass) {

                if (element.isRoot) {
                    writer.write("@XmlRootElement(name=\"" + element.name + "\"" + (element.namespace != null ? ", namespace=\"" + element.namespace + "\"" : "") + ")\n");
                    writer.write("@XmlType(name=\"\", propOrder = {" + getPropOrder(element.name, classMap) + "})\n");
                } else {
                    writer.write("@XmlType(name = \"" + element.name + "\"" + (element.namespace != null ? ", namespace=\"" + element.namespace + "\"" : "") + ", propOrder = {" + getPropOrder(element.name, classMap) + "})\n");
                }

                writer.write("@XmlAccessorType(XmlAccessType.FIELD)\n");
                writer.write("@Getter\n@Setter\n");
                writer.write((isFirstClass ? "public " : "public static ") + "class " + toClassName(element.name) + " {\n");


                for (ElementInfo child : classMap.getOrDefault(element.name, new ArrayList<>())) {
                    writer.write("    @XmlElement(name=\"" + child.name + "\"" + (child.namespace != null ? ", namespace=\"" + child.namespace + "\"" : "") + ")\n");
                    writer.write("    private " + (child.isList ? "List<" + toClassName(child.name) + ">" : (child.isClass ? toClassName(child.name) : child.type)) + " " + child.name + "" + (child.isList ? " = new ArrayList<>()" : "") + ";\n");
                }

                generateClass(writer, element.name, classMap, false);
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

    private void resetState() {
        elementList.clear();
        processedElements.clear();
        elementCount.clear();
        idCounter = 1;
    }

}
