package XmlXsdToJAXB4SOAP.component;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaFileUpdater {


    private HashMap<String, String> parseXSD(String xsdFilePath) {
        HashMap<String, String> elementNamespaceMap = new HashMap<>();

        try {
            File file = new File(xsdFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList elementList = doc.getElementsByTagName("xs:element");

            for (int i = 0; i < elementList.getLength(); i++) {
                Element element = (Element) elementList.item(i);
                NamedNodeMap attributes = element.getAttributes();

                String name = "";
                String namespace = "";

                for (int j = 0; j < attributes.getLength(); j++) {

                    String attrName = attributes.item(j).getNodeName();

                    if (attrName.startsWith("name")) {
                        name = attributes.item(j).getNodeValue();
                    }

                    if (attrName.startsWith("xmlns")) {
                        namespace = attributes.item(j).getNodeValue();
                    }



                }

                if(!namespace.isEmpty()) {
                    elementNamespaceMap.put(name,namespace);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return elementNamespaceMap;
    }


    public void updateJavaFile(String xsdFilePath, String javaFilePath) {
        try {

            HashMap<String, String> elementNamespaceMap = parseXSD(xsdFilePath);

            Path path = Paths.get(javaFilePath);
            String content = new String(Files.readAllBytes(path));

            // Remove @XmlElement(required = true) annotations
            String requiredRegex = "@XmlElement\\(required\\s*=\\s*true\\s*\\)\\s*";
            content = content.replaceAll(requiredRegex, "");

            // Her bir element ve namespace için işlemi gerçekleştir
            for (HashMap.Entry<String, String> entry : elementNamespaceMap.entrySet()) {
                String elementName = entry.getKey();
                String namespace = entry.getValue();

                // @XmlElement varsa regex ile güncelle
                String nameRegex = "@XmlElement\\(name\\s*=\\s*\"" + elementName + "\"(.*)\\)";
                Pattern namePattern = Pattern.compile(nameRegex);
                Matcher nameMatcher = namePattern.matcher(content);

                if (nameMatcher.find()) {
                    // Eğer @XmlElement zaten varsa, namespace'i ekle
                    String oldAnnotation = nameMatcher.group();
                    if (!oldAnnotation.contains("namespace")) {
                        String newAnnotation = oldAnnotation.substring(0, oldAnnotation.length() - 1) + ", namespace = \"" + namespace + "\")";
                        content = content.replace(oldAnnotation, newAnnotation);
                    }
                } else {
                    // Eğer @XmlElement yoksa, tam anotasyonu ekle
                    String newAnnotation = "@XmlElement(name = \"" + elementName + "\", required = true, namespace = \"" + namespace + "\")\n";
                    String fieldRegex = "(protected\\s+.*\\s+" + elementName + "\\s*;)";
                    content = content.replaceAll(fieldRegex, newAnnotation + "$1");
                }
            }

            // Güncellenmiş içeriği dosyaya yaz
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
