package com.example.library.controller;

import com.example.library.model.UploadedFile;
import com.example.library.service.BookFileService;
import com.example.library.storage.StoredObject;
import java.io.IOException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FileController {

    private final BookFileService bookFileService;

    public FileController(BookFileService bookFileService) {
        this.bookFileService = bookFileService;
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> show(@PathVariable Long id) throws IOException {
        UploadedFile uploadedFile = bookFileService.findMetadata(id);
        StoredObject object = bookFileService.load(uploadedFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(object.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(object.filename()).build());
        headers.setContentLength(object.bytes().length);

        return ResponseEntity.ok()
            .headers(headers)
            .body(object.bytes());
    }
}
