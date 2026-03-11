package com.example.document_service.service;

import java.io.File;
import java.io.IOException;

import io.javalin.http.UploadedFile;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class DocumentOCRService {

    private final Tesseract tesseract;

    public DocumentOCRService() {
        this.tesseract = new Tesseract();
        // OCR in italiano
        tesseract.setLanguage("ita");
        // opzionale: path dei dati linguistici
        // tesseract.setDatapath("/usr/share/tessdata");
    }

    /**
     * Converte un file caricato in testo usando OCR
     */
    public String performOCR(UploadedFile uploadedFile) throws IOException, TesseractException {
        // Creo file temporaneo
        File tempFile = File.createTempFile("ocr-", uploadedFile.filename());
        tempFile.deleteOnExit();

        try (var input = uploadedFile.content()) {
            java.nio.file.Files.copy(input, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        String text = tesseract.doOCR(tempFile);

        // Cancella subito il file temporaneo
        tempFile.delete();

        return text;
    }
}