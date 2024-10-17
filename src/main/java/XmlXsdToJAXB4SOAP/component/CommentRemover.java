package XmlXsdToJAXB4SOAP.component;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class CommentRemover {

    public void removeCommentsAndEmptyLines(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            throw new IOException("Geçersiz dosya yolu: " + filePath);
        }

        File tempFile = new File(file.getAbsolutePath() + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            boolean isMultiLineComment = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Satırı baştaki ve sondaki boşluklardan arındır

                // Çok satırlı yorum kontrolü
                if (isMultiLineComment) {
                    if (line.contains("*/")) {
                        isMultiLineComment = false;
                        line = line.substring(line.indexOf("*/") + 2).trim(); // Yorum sonundan sonraki kısmı al
                    } else {
                        continue; // Yorum içindeyiz, bu satırı atla
                    }
                }

                // Tek satırlık yorum kontrolü
                if (line.startsWith("//")) {
                    continue; // Tek satırlık yorumu atla
                }

                // Çok satırlı yorum başlangıcı kontrolü
                if (line.contains("/*")) {
                    isMultiLineComment = true;
                    line = line.substring(0, line.indexOf("/*")).trim(); // Yorum başlangıcından öncesini al
                }

                // Boş satır değilse yaz
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        // Geçici dosyayı orijinal dosya ile değiştir
        if (!file.delete()) {
            throw new IOException("Orijinal dosya silinemedi: " + filePath);
        }
        if (!tempFile.renameTo(file)) {
            throw new IOException("Geçici dosya orijinal dosya ile değiştirilemedi: " + filePath);
        }
    }
}
