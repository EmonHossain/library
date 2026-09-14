package com.example.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.library.model.Book;
import com.example.library.model.BorrowRecord;
import com.example.library.model.BorrowStatus;
import com.example.library.model.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BorrowService borrowService;

    @Test
    void borrowDecreasesAvailableCopiesAndCreatesActiveRecord() {
        User user = new User();
        user.setEmail("user@library.test");
        Book book = new Book();
        book.setTotalCopies(3);
        book.setAvailableCopies(2);

        when(userRepository.findByEmail("user@library.test")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(borrowRecordRepository.findByUserEmailAndBookIdAndStatus("user@library.test", 10L, BorrowStatus.ACTIVE))
            .thenReturn(Optional.empty());

        borrowService.borrow(10L, "user@library.test");

        assertThat(book.getAvailableCopies()).isEqualTo(1);
        ArgumentCaptor<BorrowRecord> recordCaptor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrowRecordRepository).save(recordCaptor.capture());
        BorrowRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getUser()).isSameAs(user);
        assertThat(savedRecord.getBook()).isSameAs(book);
        assertThat(savedRecord.getStatus()).isEqualTo(BorrowStatus.ACTIVE);
        assertThat(savedRecord.getBorrowedAt()).isNotNull();
        assertThat(savedRecord.getDueAt()).isAfter(savedRecord.getBorrowedAt());
    }

    @Test
    void borrowRejectsUnavailableBook() {
        User user = new User();
        Book book = new Book();
        book.setTotalCopies(1);
        book.setAvailableCopies(0);

        when(userRepository.findByEmail("user@library.test")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> borrowService.borrow(10L, "user@library.test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No copies are currently available.");

        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    void returnBookRejectsNonOwnerWhenNotAdmin() {
        User owner = new User();
        owner.setEmail("owner@library.test");
        BorrowRecord record = new BorrowRecord();
        record.setUser(owner);
        record.setStatus(BorrowStatus.ACTIVE);

        when(borrowRecordRepository.findById(4L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> borrowService.returnBook(4L, "other@library.test", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("You can only return your own borrowed books.");
    }

    @Test
    void returnBookMarksRecordReturnedAndCapsAvailableCopiesAtTotal() {
        User owner = new User();
        owner.setEmail("owner@library.test");
        Book book = new Book();
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        BorrowRecord record = new BorrowRecord();
        record.setUser(owner);
        record.setBook(book);
        record.setStatus(BorrowStatus.ACTIVE);

        when(borrowRecordRepository.findById(4L)).thenReturn(Optional.of(record));

        borrowService.returnBook(4L, "owner@library.test", false);

        assertThat(book.getAvailableCopies()).isEqualTo(2);
        assertThat(record.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(record.getReturnedAt()).isNotNull();
    }
}
