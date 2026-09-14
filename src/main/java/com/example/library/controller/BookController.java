package com.example.library.controller;

import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookController {

    private final BookService bookService;
    private final BorrowService borrowService;

    public BookController(BookService bookService, BorrowService borrowService) {
        this.bookService = bookService;
        this.borrowService = borrowService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/books";
    }

    @GetMapping("/books")
    public String books(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long categoryId,
        Model model
    ) {
        model.addAttribute("books", bookService.search(q, categoryId));
        model.addAttribute("categories", bookService.findCategories());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoryId", categoryId);
        return "books/list";
    }

    @PostMapping("/books/{id}/borrow")
    public String borrow(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            borrowService.borrow(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Book borrowed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/books";
    }

    @GetMapping("/my-borrows")
    public String myBorrows(Principal principal, Model model) {
        model.addAttribute("borrows", borrowService.findForUser(principal.getName()));
        return "borrows/my-borrows";
    }

    @PostMapping("/borrows/{id}/return")
    public String returnBorrow(
        @PathVariable Long id,
        Principal principal,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        try {
            borrowService.returnBook(id, principal.getName(), admin);
            redirectAttributes.addFlashAttribute("success", "Book returned successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/my-borrows";
    }
}
