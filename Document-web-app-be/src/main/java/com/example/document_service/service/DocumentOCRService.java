package com.example.document_service.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import io.javalin.http.UploadedFile;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class DocumentOCRService {

    private final Tesseract tesseract;

    public DocumentOCRService() {
        this.tesseract = new Tesseract();
        tesseract.setLanguage("ita");
        // tesseract.setDatapath("/usr/share/tessdata");
    }

    /**
     * Smista il file al metodo di estrazione corretto in base all'estensione.
     * - TXT: non passa da qui (gestito in DocumentService)
     * - PDF: PDFBox
     * - DOC/DOCX: Apache POI
     * - XLS/XLSX: Apache POI
     * - Immagini (PNG, JPG, ecc.): Tesseract OCR
     */
    public String performOCR(UploadedFile uploadedFile) throws IOException, TesseractException {
        String fileType = getFileExtension(uploadedFile.filename()).toUpperCase();

        return switch (fileType) {
            case "PDF"  -> extractFromPdf(uploadedFile);
            case "DOCX" -> extractFromDocx(uploadedFile);
            case "DOC"  -> extractFromDoc(uploadedFile);
            case "XLSX" -> extractFromXlsx(uploadedFile);
            case "XLS"  -> extractFromXls(uploadedFile);
            default     -> extractWithTesseract(uploadedFile); // immagini o altri formati
        };
    }

    // --- PDF ---
    private String extractFromPdf(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content()) {
            byte[] bytes = is.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
    }


    // --- DOCX ---
    private String extractFromDocx(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            document.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    // --- DOC (vecchio formato) ---
    private String extractFromDoc(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content();
             HWPFDocument document = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(document)) { // aggiunto al try → chiuso automaticamente
            return extractor.getText();
        }
    }

    // --- XLSX ---
    private String extractFromXlsx(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content();
             Workbook workbook = new XSSFWorkbook(is)) {
            return extractFromWorkbook(workbook);
        }
    }

    // --- XLS (vecchio formato) ---
    private String extractFromXls(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content();
             Workbook workbook = new HSSFWorkbook(is)) {
            return extractFromWorkbook(workbook);
        }
    }

    private String extractFromWorkbook(Workbook workbook) {
        StringBuilder sb = new StringBuilder();
        for (Sheet sheet : workbook) {
            sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(cell.toString()).append("\t");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    // --- Fallback Tesseract (immagini) ---
    private String extractWithTesseract(UploadedFile uploadedFile) throws IOException, TesseractException {
        File tempFile = File.createTempFile("ocr-", uploadedFile.filename());
        tempFile.deleteOnExit();

        try (InputStream is = uploadedFile.content()) {
            java.nio.file.Files.copy(is, tempFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            return tesseract.doOCR(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx < 0) ? "" : filename.substring(idx + 1);
    }
}