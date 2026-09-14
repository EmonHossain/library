package com.example.library.service;

import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.BorrowStatus;
import com.example.library.model.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BorrowService(
        BorrowRecordRepository borrowRecordRepository,
        BookRepository bookRepository,
        UserRepository userRepository
    ) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void borrow(Long bookId, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book not found."));

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalArgumentException("No copies are currently available.");
        }

        borrowRecordRepository.findByUserEmailAndBookIdAndStatus(email, bookId, BorrowStatus.ACTIVE)
            .ifPresent(existing -> {
                throw new IllegalArgumentException("You already have this book checked out.");
            });

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowedAt(LocalDateTime.now());
        record.setDueAt(LocalDateTime.now().plusDays(14));
        record.setStatus(BorrowStatus.ACTIVE);
        borrowRecordRepository.save(record);
    }

    @Transactional
    public void returnBook(Long borrowId, String email, boolean admin) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
            .orElseThrow(() -> new IllegalArgumentException("Borrow record not found."));

        if (!admin && !record.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("You can only return your own borrowed books.");
        }
        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalArgumentException("This book was already returned.");
        }

        Book book = record.getBook();
        book.setAvailableCopies(Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1));
        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> findForUser(String email) {
        return borrowRecordRepository.findByUserEmailWithDetails(email);
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> findAll() {
        return borrowRecordRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public long countActiveBorrows() {
        return borrowRecordRepository.countByStatus(BorrowStatus.ACTIVE);
    }
}
