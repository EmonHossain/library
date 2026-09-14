package com.example.library.service;

import com.example.library.model.FilePurpose;
import com.example.library.model.UploadedFile;
import com.example.library.repository.UploadedFileRepository;
import com.example.library.storage.StorageService;
import com.example.library.storage.StoredObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookFileService {

    private final StorageService storageService;
    private final UploadedFileRepository uploadedFileRepository;

    public BookFileService(StorageService storageService, UploadedFileRepository uploadedFileRepository) {
        this.storageService = storageService;
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @Transactional
    public UploadedFile storePdf(MultipartFile file) {
        validatePdf(file);
        return store(file, FilePurpose.BOOK_PDF);
    }

    @Transactional
    public UploadedFile storeCoverImage(MultipartFile file) {
        validateCoverImage(file);
        return store(file, FilePurpose.COVER_IMAGE);
    }

    public void validatePdf(MultipartFile file) {
        validate(file, FilePurpose.BOOK_PDF);
    }

    public void validateCoverImage(MultipartFile file) {
        validate(file, FilePurpose.COVER_IMAGE);
    }

    @Transactional(readOnly = true)
    public UploadedFile findMetadata(Long id) {
        return uploadedFileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found."));
    }

    public StoredObject load(UploadedFile uploadedFile) throws IOException {
        return storageService.load(
            uploadedFile.getStorageKey(),
            uploadedFile.getContentType(),
            uploadedFile.getOriginalFilename()
        );
    }

    private UploadedFile store(MultipartFile file, FilePurpose purpose) {
        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String contentType = normalizeContentType(file);
        String key = LocalDate.now() + "/" + purpose.name().toLowerCase(Locale.ROOT) + "/"
            + UUID.randomUUID() + "-" + originalFilename;

        try {
            storageService.store(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded file.", ex);
        }

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setOriginalFilename(originalFilename);
        uploadedFile.setStorageKey(key);
        uploadedFile.setStorageType(storageService.storageType());
        uploadedFile.setContentType(contentType);
        uploadedFile.setSizeBytes(file.getSize());
        uploadedFile.setPurpose(purpose);
        return uploadedFileRepository.save(uploadedFile);
    }

    private void validate(MultipartFile file, FilePurpose purpose) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a " + label(purpose) + ".");
        }
        String filename = normalizeFilename(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String contentType = normalizeContentType(file).toLowerCase(Locale.ROOT);

        if (purpose == FilePurpose.BOOK_PDF && !contentType.equals("application/pdf") && !filename.endsWith(".pdf")) {
            throw new IllegalArgumentException("Book file must be a PDF.");
        }
        if (purpose == FilePurpose.COVER_IMAGE && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Cover file must be an image.");
        }
    }

    private String label(FilePurpose purpose) {
        return purpose == FilePurpose.BOOK_PDF ? "book PDF" : "cover image";
    }

    private String normalizeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "upload" : filename;
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String normalizeContentType(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
            ? "application/octet-stream"
            : file.getContentType();
    }
}
