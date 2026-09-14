package com.example.library.service;

import com.example.library.dto.BookForm;
import com.example.library.model.Book;
import com.example.library.model.BorrowStatus;
import com.example.library.model.Category;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookFileService bookFileService;

    public BookService(
        BookRepository bookRepository,
        CategoryRepository categoryRepository,
        BorrowRecordRepository borrowRecordRepository,
        BookFileService bookFileService
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookFileService = bookFileService;
    }

    @Transactional(readOnly = true)
    public List<Book> search(String query, Long categoryId) {
        return bookRepository.search(query == null ? "" : query.trim(), categoryId);
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Book findBook(Long id) {
        return bookRepository.findWithCategoryById(id)
            .orElseThrow(() -> new IllegalArgumentException("Book not found."));
    }

    @Transactional
    public void create(BookForm form) {
        bookFileService.validatePdf(form.getPdfFile());
        bookFileService.validateCoverImage(form.getCoverImageFile());

        Book book = new Book();
        applyForm(book, form);
        book.setAvailableCopies(form.getTotalCopies());
        book.setPdfFile(bookFileService.storePdf(form.getPdfFile()));
        book.setCoverImageFile(bookFileService.storeCoverImage(form.getCoverImageFile()));
        bookRepository.save(book);
    }

    @Transactional
    public void update(Long id, BookForm form) {
        Book book = findBook(id);
        long activeBorrows = borrowRecordRepository.countByBookIdAndStatus(id, BorrowStatus.ACTIVE);
        if (form.getTotalCopies() < activeBorrows) {
            throw new IllegalArgumentException("Total copies cannot be less than active borrowed copies.");
        }

        applyForm(book, form);
        if (hasUpload(form.getPdfFile())) {
            book.setPdfFile(bookFileService.storePdf(form.getPdfFile()));
        }
        if (hasUpload(form.getCoverImageFile())) {
            book.setCoverImageFile(bookFileService.storeCoverImage(form.getCoverImageFile()));
        }
        book.setAvailableCopies(form.getTotalCopies() - (int) activeBorrows);
    }

    @Transactional
    public void delete(Long id) {
        long activeBorrows = borrowRecordRepository.countByBookIdAndStatus(id, BorrowStatus.ACTIVE);
        if (activeBorrows > 0) {
            throw new IllegalArgumentException("Books with active borrows cannot be deleted.");
        }
        bookRepository.deleteById(id);
    }

    public BookForm toForm(Book book) {
        BookForm form = new BookForm();
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setIsbn(book.getIsbn());
        form.setDescription(book.getDescription());
        form.setTotalCopies(book.getTotalCopies());
        form.setCategoryId(book.getCategory().getId());
        return form;
    }

    private void applyForm(Book book, BookForm form) {
        Category category = categoryRepository.findById(form.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        book.setTitle(form.getTitle().trim());
        book.setAuthor(form.getAuthor().trim());
        book.setIsbn(form.getIsbn().trim());
        book.setDescription(form.getDescription() == null ? "" : form.getDescription().trim());
        book.setTotalCopies(form.getTotalCopies());
        book.setCategory(category);
    }

    private boolean hasUpload(org.springframework.web.multipart.MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
