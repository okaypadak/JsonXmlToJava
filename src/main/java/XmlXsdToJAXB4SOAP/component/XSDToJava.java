package XmlXsdToJAXB4SOAP.component;

import com.sun.tools.xjc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;
import java.io.File;

@Component
@Slf4j
public class XSDToJava {

    public void convert(File file, String outputDir) {


        try {

            String[] args = new String[]{
                    "-d", outputDir,
                    "-p", "java",
                    file.getAbsolutePath()
            };

            XJCListener listener = new XJCListener() {
                @Override
                public void error(SAXParseException e) {
                    log.error("Error: {}",e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) {
                    log.error("Error: {}",e.getMessage());
                }

                @Override
                public void warning(SAXParseException e) {
                    log.error("Error: {}",e.getMessage());
                }

                @Override
                public void info(SAXParseException e) {
                    log.error("Error: {}",e.getMessage());
                }
            };

            Driver.run(args, listener);

        } catch (Throwable e) {
            log.error("Error: {}",e.getMessage());
        }

    }
}
