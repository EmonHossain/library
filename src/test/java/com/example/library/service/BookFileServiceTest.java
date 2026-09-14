package com.example.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.library.model.FilePurpose;
import com.example.library.model.UploadedFile;
import com.example.library.repository.UploadedFileRepository;
import com.example.library.storage.StorageService;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BookFileServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @InjectMocks
    private BookFileService bookFileService;

    @Test
    void storePdfWritesToStorageAndPersistsMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "pdfFile",
            "Clean Code.pdf",
            "application/pdf",
            "pdf-bytes".getBytes()
        );

        when(storageService.storageType()).thenReturn("local");
        when(uploadedFileRepository.save(any(UploadedFile.class))).then(returnsFirstArg());

        UploadedFile uploadedFile = bookFileService.storePdf(file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).store(
            keyCaptor.capture(),
            any(InputStream.class),
            eq(file.getSize()),
            eq("application/pdf")
        );
        assertThat(keyCaptor.getValue()).contains("/book_pdf/").endsWith("-Clean_Code.pdf");
        assertThat(uploadedFile.getOriginalFilename()).isEqualTo("Clean_Code.pdf");
        assertThat(uploadedFile.getStorageType()).isEqualTo("local");
        assertThat(uploadedFile.getContentType()).isEqualTo("application/pdf");
        assertThat(uploadedFile.getSizeBytes()).isEqualTo(file.getSize());
        assertThat(uploadedFile.getPurpose()).isEqualTo(FilePurpose.BOOK_PDF);
    }

    @Test
    void validatePdfRejectsNonPdfUpload() {
        MockMultipartFile file = new MockMultipartFile(
            "pdfFile",
            "notes.txt",
            "text/plain",
            "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> bookFileService.validatePdf(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Book file must be a PDF.");
    }

    @Test
    void validateCoverImageRejectsNonImageUpload() {
        MockMultipartFile file = new MockMultipartFile(
            "coverImageFile",
            "cover.pdf",
            "application/pdf",
            "not an image".getBytes()
        );

        assertThatThrownBy(() -> bookFileService.validateCoverImage(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cover file must be an image.");
    }
}
