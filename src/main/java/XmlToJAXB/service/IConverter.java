package XmlToJAXB.service;

import XmlToJAXB.exception.ProcessingException;

import java.io.File;

public interface IConverter {
    void convert(File file, String fullOutputDir)  throws ProcessingException;
}
