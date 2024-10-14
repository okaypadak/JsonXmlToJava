package XmlXsdToJAXB4SOAP.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

@Service
public class XmlToXsdService {

    public String convert(File xmlFile) {
        String xsdFile = xmlFile.toString().replace(".xml", ".xsd");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            StringBuilder xsdContent = new StringBuilder();
            xsdContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xsdContent.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n");

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

    private static String createXsdElement(Element element) {
        StringBuilder xsd = new StringBuilder();
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
            xsd.append("<xs:element name=\"").append(elementName).append("\">\n");
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
            xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\"/>\n");
        }

        return xsd.toString();
    }

    private static String createXsdChildElement(Element element) {
        StringBuilder xsd = new StringBuilder();
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
            xsd.append("<xs:element name=\"").append(elementName).append("\">\n");
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
            xsd.append("<xs:element name=\"").append(elementName).append("\" type=\"").append(type).append("\"/>\n");
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
}
