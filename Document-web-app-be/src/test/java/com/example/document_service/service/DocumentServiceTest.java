package com.example.document_service.service; // <-- corretto

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.document_service.event.DocumentEventPublisher;
import com.example.document_service.model.Document;
import com.example.document_service.repository.DocumentRepository;

class DocumentServiceTest {

    private DocumentRepository repository;
    private DocumentEventPublisher eventPublisher;
    private DocumentOCRService ocrService;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentRepository.class);
        eventPublisher = mock(DocumentEventPublisher.class);
        ocrService = mock(DocumentOCRService.class);
        documentService = new DocumentService(repository, eventPublisher, ocrService);
    }

    @Test
    void testUploadDocumentTxt() {
        long userId = 1L;
        String filename = "test1.txt";
        String content = "Hello world";

        // Restituisce il documento passato come argomento
        when(repository.create(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Document doc = documentService.uploadDocument(userId, filename, content, "text/plain");

        assertThat(doc.getFilename()).isEqualTo(filename);
        assertThat(doc.getUserId()).isEqualTo(userId);
        assertThat(doc.getContent()).isEqualTo(content);

        verify(repository).create(any(Document.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void testUploadDocumentInvalidUserId() {
        assertThatThrownBy(() -> documentService.uploadDocument(0L, "test.txt", "content", "text/plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void testUploadDocumentInvalidExtension() {
        assertThatThrownBy(() -> documentService.uploadDocument(1L, "test.exe", "content", "text/plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estensione");
    }

    @Test
    void testGetUserDocuments() {
        long userId = 1L;

        when(repository.findByUserId(userId, 0, 20, false))
                .thenReturn(List.of(new Document()));

        List<Document> docs = documentService.getUserDocuments(userId);

        assertThat(docs).hasSize(1);
        verify(repository).findByUserId(userId, 0, 20, false);
    }

    @Test
    void testDeleteDocument() {
        long userId = 1L;
        String docId = "64a1c2b8f1f0c123456789ab";

        Document doc = new Document();
        doc.setId(docId);
        doc.setUserId(userId);

        when(repository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(doc));
        when(repository.deleteByIdAndUserId(docId, userId)).thenReturn(true);

        boolean deleted = documentService.deleteDocument(userId, docId);

        assertThat(deleted).isTrue();
        verify(eventPublisher).publish(any());
    }

    @Test
    void testDeleteDocumentNotFound() {
        when(repository.findByIdAndUserId("nonexistent", 1L)).thenReturn(Optional.empty());
        when(repository.deleteByIdAndUserId("nonexistent", 1L)).thenReturn(false);

        boolean deleted = documentService.deleteDocument(1L, "nonexistent");

        assertThat(deleted).isFalse();
        verify(eventPublisher, never()).publish(any());
    }
}