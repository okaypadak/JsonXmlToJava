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



    public List<OperationWithXml> convert(File wsdlFile) {

        List<OperationWithXml> operationXmlList = new ArrayList<>();
        List<OperationData> operations = extractOperations(wsdlFile);
        List<MessageData> messages = extractMessages(wsdlFile);
        List<XsdData> xsdData = generateXmlFromXsd(wsdlFile);

        for (OperationData operation : operations) {
            String inputMessage = removeNamespace(operation.getInputMessage());
            String outputMessage = removeNamespace(operation.getOutputMessage());
            String requestXml = findXmlByMessage(inputMessage, messages, xsdData);
            String responseXml = findXmlByMessage(outputMessage, messages, xsdData);
            operationXmlList.add(new OperationWithXml(operation.getMethodName(), requestXml, responseXml));
        }
        return operationXmlList;
    }

    private String findXmlByMessage(String messageName, List<MessageData> messages, List<XsdData> xsdData) {
        for (MessageData message : messages) {
            if (message.getMessageName().contains(messageName)) {
                for (XsdData xsd : xsdData) {
                    String MessageName = removeNamespace(message.getElementName());
                    if (xsd.getElementName().equals(MessageName)) {
                        return xsd.getXmlContent();
                    }
                }
            }
        }
        return "<error>XML not found</error>";
    }


    public List<OperationData> extractOperations(File wsdlFile) {
        List<OperationData> operationDataList = new ArrayList<>();
        try {
            Document doc = parseXml(wsdlFile);
            NodeList operationNodes = evaluateXPath(doc, "//*[local-name()='portType']/*[local-name()='operation']");
            for (int i = 0; i < operationNodes.getLength(); i++) {
                Node operation = operationNodes.item(i);
                String methodName = operation.getAttributes().getNamedItem("name").getNodeValue();
                Node inputNode = evaluateXPath(operation, "*[local-name()='input']").item(0);
                Node outputNode = evaluateXPath(operation, "*[local-name()='output']").item(0);
                String inputMessage = (inputNode != null && inputNode.getAttributes().getNamedItem("message") != null) ? inputNode.getAttributes().getNamedItem("message").getNodeValue() : "N/A";
                String outputMessage = (outputNode != null && outputNode.getAttributes().getNamedItem("message") != null) ? outputNode.getAttributes().getNamedItem("message").getNodeValue() : "N/A";
                operationDataList.add(new OperationData(methodName, inputMessage, outputMessage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return operationDataList;
    }


    public List<MessageData> extractMessages(File wsdlFile) {
        List<MessageData> messageDataList = new ArrayList<>();
        try {
            Document doc = parseXml(wsdlFile);
            NodeList messageNodes = evaluateXPath(doc, "//*[local-name()='message']");
            for (int i = 0; i < messageNodes.getLength(); i++) {
                Node message = messageNodes.item(i);
                String messageName = message.getAttributes().getNamedItem("name").getNodeValue();
                Node partNode = evaluateXPath(message, "*[local-name()='part']").item(0);
                String elementName = (partNode != null && partNode.getAttributes().getNamedItem("element") != null) ? partNode.getAttributes().getNamedItem("element").getNodeValue() : "N/A";
                String partName = (partNode != null && partNode.getAttributes().getNamedItem("name") != null) ? partNode.getAttributes().getNamedItem("name").getNodeValue() : "N/A";
                messageDataList.add(new MessageData(messageName, elementName, partName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageDataList;
    }


    public List<XsdData> generateXmlFromXsd(File wsdlFile) {
        List<XsdData> xsdDataList = new ArrayList<>();
        try {
            Document doc = parseXml(wsdlFile);
            NodeList schemaNodes = evaluateXPath(doc, "//*[local-name()='schema']/*[local-name()='element']");
            for (int i = 0; i < schemaNodes.getLength(); i++) {
                Node element = schemaNodes.item(i);
                String elementName = element.getAttributes().getNamedItem("name").getNodeValue();
                String xmlContent = buildXmlFromElement(element);
                xsdDataList.add(new XsdData(elementName, xmlContent));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xsdDataList;
    }

    private String buildXmlFromElement(Node element) {
        StringBuilder xmlBuilder = new StringBuilder();
        String elementName = element.getAttributes().getNamedItem("name").getNodeValue();
        xmlBuilder.append("<").append(elementName).append(">");
        NodeList children = evaluateXPathSafe(element, "*[local-name()='complexType']/*[local-name()='sequence']/*[local-name()='element']");
        if (children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                xmlBuilder.append(buildXmlFromElement(children.item(i)));
            }
        } else {
            xmlBuilder.append("...");
        }
        xmlBuilder.append("</").append(elementName).append(">");
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
                @Override public Node item(int index) { return null; }
                @Override public int getLength() { return 0; }
            };
        }
    }

    private String removeNamespace(String value) {
        return value.contains(":") ? value.substring(value.indexOf(":") + 1) : value;
    }


    public static class OperationData {
        private String methodName;
        private String inputMessage;
        private String outputMessage;

        public OperationData(String methodName, String inputMessage, String outputMessage) {
            this.methodName = methodName;
            this.inputMessage = inputMessage;
            this.outputMessage = outputMessage;
        }

        public String getMethodName() { return methodName; }
        public String getInputMessage() { return inputMessage; }
        public String getOutputMessage() { return outputMessage; }
    }

    public static class MessageData {
        private String messageName;
        private String elementName;
        private String partName;

        public MessageData(String messageName, String elementName, String partName) {
            this.messageName = messageName;
            this.elementName = elementName;
            this.partName = partName;
        }

        public String getMessageName() { return messageName; }
        public String getElementName() { return elementName; }
        public String getPartName() { return partName; }
    }

    public static class XsdData {
        private String elementName;
        private String xmlContent;

        public XsdData(String elementName, String xmlContent) {
            this.elementName = elementName;
            this.xmlContent = xmlContent;
        }

        public String getElementName() { return elementName; }
        public String getXmlContent() { return xmlContent; }
    }


    public static class OperationWithXml {
        private String methodName;
        private String requestXml;
        private String responseXml;

        public OperationWithXml(String methodName, String requestXml, String responseXml) {
            this.methodName = methodName;
            this.requestXml = requestXml;
            this.responseXml = responseXml;
        }

        public String getMethodName() { return methodName; }
        public String getRequestXml() { return requestXml; }
        public String getResponseXml() { return responseXml; }
    }
}
