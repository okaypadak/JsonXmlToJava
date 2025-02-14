package XmlToJAXB.component;

import XmlToJAXB.exception.XmlProcessingException;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


@Service
public class XmlToJava {

    private List<ElementInfo> elementList = new ArrayList<>();
    private int idCounter = 1;
    private Set<String> processedElements = new HashSet<>();
    private Map<String, Integer> elementCount = new HashMap<>();

    public void convert(File xmlFile, String fullOutputDir) throws XmlProcessingException {
        try {
            resetState();
            parseXML(xmlFile);
            generateJAXBClass(fullOutputDir, xmlFile.getName());
        } catch (Exception e) {
            throw new XmlProcessingException("Bu XML sorunludur: " + e.getMessage());
        }
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
        List<AttributeInfo> attributes = new ArrayList<>();

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

    private class AttributeInfo {
        String name;
        String type;

        AttributeInfo(String name, String type) {
            this.name = name;
            this.type = type;
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

        if (isEmptyElement(element) || hasOnlyEmptyChildren(element)) {
            return;
        }

        String namespaceURI = element.getNamespaceURI();
        String localName = element.getLocalName();
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

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                String childName = node.getLocalName();
                String childKey;

                if (localName.equals(childName)) {
                    childKey = localName + ":" + childName + "Sub";
                } else {
                    childKey = localName + ":" + childName;
                }

                elementCount.put(childKey, elementCount.getOrDefault(childKey, 0) + 1);
                isClass = true;
            }
        }

        if (!isClass) {
            String content = element.getTextContent().trim();
            if (content.matches("\\d+")) {
                type = "Integer";
            } else if (content.matches("\\d+\\.\\d+")) {
                type = "BigDecimal";
            }
        } else {
            type = toClassName(localName);
        }

        isList = elementCount.getOrDefault(elementKey, 0) > 1;
        ElementInfo elementInfo = new ElementInfo(idCounter++, localName, namespaceURI, parent, isClass, isRoot, type, isList);


        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrType = "String";

            if (attr.getNodeValue().matches("\\d+")) {
                attrType = "Integer";
            } else if (attr.getNodeValue().matches("\\d+\\.\\d+")) {
                attrType = "BigDecimal";
            }

            elementInfo.attributes.add(new AttributeInfo(attrName, attrType));
        }

        elementList.add(elementInfo);

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

            generateClass(name, writer, null, classMap, true);
        }
    }

    private void generateClass(String name, FileWriter writer, String parent, Map<String, List<ElementInfo>> classMap, boolean isFirstClass) throws Exception {

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

                writer.write((isFirstClass ? "public " : "public static ") + "class " + (isFirstClass ? toClassName(name.replace(".xml","")) : toClassName(element.name)) + " {\n");


                for (AttributeInfo attr : element.attributes) {
                    writer.write("    @XmlAttribute(name=\"" + attr.name + "\")\n");
                    writer.write("    private " + attr.type + " " + attr.name + ";\n");
                }

                for (ElementInfo child : classMap.getOrDefault(element.name, new ArrayList<>())) {
                    writer.write("    @XmlElement(name=\"" + child.name + "\"" + (child.namespace != null ? ", namespace=\"" + child.namespace + "\"" : "") + ")\n");
                    writer.write("    private " + (child.isList ? "List<" + toClassName(child.name) + ">" : (child.isClass ? toClassName(child.name) : child.type)) + " " + child.name + "" + (child.isList ? " = new ArrayList<>()" : "") + ";\n");
                }

                generateClass(name, writer, element.name, classMap, false);
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

    private boolean isEmptyElement(Element element) {

        if (element.getChildNodes().getLength() == 0) {
            return true;
        }

        if (element.getChildNodes().getLength() == 1 &&
                element.getFirstChild().getNodeType() == Node.TEXT_NODE &&
                element.getTextContent().trim().isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean hasOnlyEmptyChildren(Element element) {

        NodeList children = element.getChildNodes();

        boolean hasChildElements = false;
        boolean allChildrenEmpty = true;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                hasChildElements = true;
                if (!isEmptyElement((Element) node)) {
                    allChildrenEmpty = false;
                }
            }
        }

        return hasChildElements && allChildrenEmpty;
    }

    private void resetState() {
        elementList.clear();
        processedElements.clear();
        elementCount.clear();
        idCounter = 1;
    }

}
