package XmlToJAXB.component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class WsdlGenerate {

    public List<String[]> extractOperations(File wsdlFile) {
        List<String[]> operationData = new ArrayList<>();
        try {
            Document doc = parseXml(wsdlFile);
            NodeList operationNodes = evaluateXPath(doc, "//*[local-name()='portType']/*[local-name()='operation']");
            for (int i = 0; i < operationNodes.getLength(); i++) {
                Node operation = operationNodes.item(i);
                String methodName = operation.getAttributes().getNamedItem("name").getNodeValue();
                Node inputNode = evaluateXPath(operation, "*[local-name()='input']").item(0);
                Node outputNode = evaluateXPath(operation, "*[local-name()='output']").item(0);
                String inputMessage = (inputNode != null && inputNode.getAttributes().getNamedItem("message") != null)
                        ? inputNode.getAttributes().getNamedItem("message").getNodeValue() : "N/A";
                String outputMessage = (outputNode != null && outputNode.getAttributes().getNamedItem("message") != null)
                        ? outputNode.getAttributes().getNamedItem("message").getNodeValue() : "N/A";
                operationData.add(new String[]{methodName, inputMessage, outputMessage});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return operationData;
    }

    public String generateXmlFromXsd(File wsdlFile) {
        try {
            Document doc = parseXml(wsdlFile);
            NodeList schemaNodes = evaluateXPath(doc, "//*[local-name()='schema']/*[local-name()='element']");
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            for (int i = 0; i < schemaNodes.getLength(); i++) {
                Node element = schemaNodes.item(i);
                xmlBuilder.append(buildXmlFromElement(element, 0));
            }
            System.out.println(xmlBuilder.toString());
            return xmlBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<error>Could not generate XML from XSD</error>";
    }

    private String buildXmlFromElement(Node element, int depth) {
        StringBuilder xmlBuilder = new StringBuilder();
        String elementName = element.getAttributes().getNamedItem("name").getNodeValue();
        xmlBuilder.append(indent(depth)).append("<").append(elementName).append(">");
        NodeList children = evaluateXPathSafe(element, "*[local-name()='complexType']/*[local-name()='sequence']/*[local-name()='element']");
        if (children.getLength() > 0) {
            xmlBuilder.append("\n");
            for (int i = 0; i < children.getLength(); i++) {
                xmlBuilder.append(buildXmlFromElement(children.item(i), depth + 1));
            }
            xmlBuilder.append(indent(depth));
        } else {
            xmlBuilder.append("...");
        }
        xmlBuilder.append("</").append(elementName).append(">\n");
        return xmlBuilder.toString();
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private NodeList evaluateXPath(Node node, String expression) throws Exception {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression xpathExpression = xpath.compile(expression);
        return (NodeList) xpathExpression.evaluate(node, XPathConstants.NODESET);
    }

    private NodeList evaluateXPathSafe(Node node, String expression) {
        try {
            return evaluateXPath(node, expression);
        } catch (Exception e) {
            return new NodeList() {
                @Override 
                public Node item(int index) { return null; }
                @Override 
                public int getLength() { return 0; }
            };
        }
    }

    private String indent(int depth) {
        return "  ".repeat(Math.max(0, depth));
    }
}
