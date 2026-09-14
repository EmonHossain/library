package com.example.library.repository;

import com.example.library.model.BorrowRecord;
import com.example.library.model.BorrowStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserEmailOrderByBorrowedAtDesc(String email);

    List<BorrowRecord> findAllByOrderByBorrowedAtDesc();

    Optional<BorrowRecord> findByUserEmailAndBookIdAndStatus(String email, Long bookId, BorrowStatus status);

    long countByBookIdAndStatus(Long bookId, BorrowStatus status);

    long countByStatus(BorrowStatus status);

    @Query("select br from BorrowRecord br join fetch br.user join fetch br.book b join fetch b.category order by br.borrowedAt desc")
    List<BorrowRecord> findAllWithDetails();

    @Query("select br from BorrowRecord br join fetch br.user join fetch br.book b join fetch b.category where br.user.email = :email order by br.borrowedAt desc")
    List<BorrowRecord> findByUserEmailWithDetails(@Param("email") String email);
}
