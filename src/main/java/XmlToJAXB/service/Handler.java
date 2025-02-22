package XmlToJAXB.service;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class Handler {

    private final Map<String, IConverter> serviceMap;

    public Handler(Map<String, IConverter> serviceMap) {
        this.serviceMap = serviceMap;
    }

    public IConverter get(String fileName) {
        String fileExtension = getName(fileName);
        IConverter converter = serviceMap.get(fileExtension);

        if (converter == null) {
            throw new IllegalArgumentException("Desteklenmeyen dosya formatı: " + fileExtension);
        }

        return converter;
    }

    private String getName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Geçersiz dosya adı: " + fileName);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase().concat("Generate");
    }
}
