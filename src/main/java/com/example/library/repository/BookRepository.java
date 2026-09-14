package com.example.library.repository;

import com.example.library.model.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
        select b from Book b
        join fetch b.category c
        left join fetch b.pdfFile
        left join fetch b.coverImageFile
        where (:categoryId is null or c.id = :categoryId)
          and (
            :query is null or :query = ''
            or lower(b.title) like lower(concat('%', :query, '%'))
            or lower(b.author) like lower(concat('%', :query, '%'))
            or lower(b.isbn) like lower(concat('%', :query, '%'))
            or lower(c.name) like lower(concat('%', :query, '%'))
          )
        order by b.title asc
        """)
    List<Book> search(@Param("query") String query, @Param("categoryId") Long categoryId);

    @Query("select b from Book b join fetch b.category left join fetch b.pdfFile left join fetch b.coverImageFile where b.id = :id")
    Optional<Book> findWithCategoryById(@Param("id") Long id);
}
