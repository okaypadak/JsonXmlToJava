package XmlXsdToJAXB4SOAP.component;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class XmlToXsd {

    private static HashMap<String, String> namespaceMap = new HashMap<>();

    public String convert(File xmlFile) {
        String xsdFile = xmlFile.toString().replace(".xml", ".xsd");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Namespace'lerin farkında ol
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            StringBuilder xsdContent = new StringBuilder();
            xsdContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xsdContent.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\"\n");

            addNamespaces(document, xsdContent);
            extractNamespaces(document);

            Element root = document.getDocumentElement();
            xsdContent.append(createXsdElement(root));

            xsdContent.append("</xs:schema>");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(xsdFile))) {
                writer.write(xsdContent.toString());
            }

            System.out.println("XSD dosyası başarıyla oluşturuldu: " + xsdFile);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return xsdFile;
    }

    private void addNamespaces(Document document, StringBuilder xsdContent) {
        NamedNodeMap namespaces = document.getDocumentElement().getAttributes();
        for (int i = 0; i < namespaces.getLength(); i++) {
            Node attr = namespaces.item(i);
            if (attr.getNodeName().startsWith("xmlns:")) {
                xsdContent.append("    ").append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append("\"\n");
            }
        }
        xsdContent.append(">\n");
    }

    private void extractNamespaces(Document document) {
        NamedNodeMap attributes = document.getDocumentElement().getAttributes();

        // Define regex pattern for extracting the namespace prefix and URI
        Pattern pattern = Pattern.compile("xmlns:(\\w+)=\"([^\"]+)\"");

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            // Check if the attribute is a namespace declaration
            if (attrName.startsWith("xmlns:")) {
                String fullNamespace = attrName + "=\"" + attrValue + "\"";
                Matcher matcher = pattern.matcher(fullNamespace);
                if (matcher.find()) {
                    String prefix = matcher.group(1); // Namespace prefix
                    String uri = matcher.group(2); // Namespace URI

                    // Add the namespace to the HashMap
                    namespaceMap.put(prefix, uri);
                }
            }
        }
    }

    private static String createXsdElement(Element element) {
        StringBuilder xsd = new StringBuilder();
        String namespace = element.getNodeName().replaceAll(":.*?(\\\\s|$)", "");
        String elementName = element.getNodeName().replaceAll("^[^:]*:", "");

        NodeList children = element.getChildNodes();
        boolean hasChildElements = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
                break;
            }
        }

        if (hasChildElements) {
            // Namespace kontrolü yap ve varsa ekle
            if (namespaceMap.containsKey(namespace)) {
                xsd.append("<xs:element name=\"").append(elementName).append("\" xmlns:").append(namespace).append("=\"").append(namespaceMap.get(namespace)).append("\">\n");
            } else {
                xsd.append("<xs:element name=\"").append(elementName).append("\">\n");
            }

            xsd.append("<xs:complexType>\n<xs:sequence>\n");

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    xsd.append(createXsdChildElement((Element) child));
                }
            }

            xsd.append("</xs:sequence>\n</xs:complexType>\n");
            xsd.append("</xs:element>\n");
        } else {
            String type = inferDataType(element.getTextContent());

            // Namespace kontrolü yap ve varsa ekle
            if (namespaceMap.containsKey(namespace)) {
                xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\" xmlns:").append(namespace).append("=\"").append(namespaceMap.get(namespace)).append("\"/>\n");
            } else {
                xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\"/>\n");
            }
        }

        return xsd.toString();
    }

    private static String createXsdChildElement(Element element) {
        StringBuilder xsd = new StringBuilder();
        String namespace = element.getNodeName().replaceAll(":.*?(\\\\s|$)", "");
        String elementName = element.getNodeName().replaceAll("^[^:]*:", "");

        NodeList children = element.getChildNodes();
        boolean hasChildElements = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
                break;
            }
        }

        if (hasChildElements) {
            if (namespaceMap.containsKey(namespace)) {
                xsd.append("<xs:element name=\"").append(elementName).append("\" xmlns:").append(namespace).append("=\"").append(namespaceMap.get(namespace)).append("\">\n");
            } else {
                xsd.append("<xs:element name=\"").append(elementName).append("\">\n");
            }

            xsd.append("<xs:complexType>\n<xs:sequence>\n");

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    xsd.append(createXsdChildElement((Element) child));
                }
            }

            xsd.append("</xs:sequence>\n</xs:complexType>\n</xs:element>\n");
        } else {
            String type = inferDataType(element.getTextContent());
            if (namespaceMap.containsKey(namespace)) {
                xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\" xmlns:").append(namespace).append("=\"").append(namespaceMap.get(namespace)).append("\"/>\n");
            } else {
                xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\"/>\n");
            }
        }

        return xsd.toString();
    }

    private static String inferDataType(String content) {
        if (content == null || content.isEmpty()) {
            return "xs:string";
        }

        if (content.equalsIgnoreCase("true") || content.equalsIgnoreCase("false")) {
            return "xs:boolean";
        }

        try {
            Integer.parseInt(content);
            return "xs:int";
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(content);
            return "xs:decimal";
        } catch (NumberFormatException ignored) {
        }

        if (content.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "xs:date";
        }
        if (content.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
            return "xs:dateTime";
        }

        return "xs:string";
    }

    public static boolean checkElementName(Element element) {
        String nodeName = element.getNodeName();
        return nodeName.contains(":");
    }
}
