package XmlToJAXB.component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.springframework.stereotype.Service;

@Service
public class JavaFormatService {

    // CodeFormatter instance for formatting Java source code
    private final CodeFormatter formatter;


    public JavaFormatService() {
        // Create a CodeFormatter with default formatting settings
        this.formatter = ToolFactory.createCodeFormatter(Map.of());
    }


    public void formatAndSaveJavaFile(String outputPath, String fileName) throws IOException {

        File file = new File(outputPath, fileName.replaceAll("\\.(json|xml)$", ".java"));

        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found or is not a valid file: " + file.getAbsolutePath());
        }

        Path filePath = file.toPath();
        String sourceCode = Files.readString(filePath, StandardCharsets.UTF_8);
        String formattedCode = formatJavaSource(sourceCode);
        Files.writeString(filePath, formattedCode, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String formatJavaSource(String source) throws IOException {

        TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);

        if (edit == null) {
            throw new IOException("Failed to format code, please check your source.");
        }

        Document document = new Document(source);
        try {
            edit.apply(document);
        } catch (BadLocationException e) {
            throw new IOException("Error occurred while formatting the code.", e);
        }

        return document.get();
    }
}
