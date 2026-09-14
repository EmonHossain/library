package com.example.library.controller;

import com.example.library.dto.BookForm;
import com.example.library.model.Book;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final BookService bookService;
    private final BorrowService borrowService;
    private final UserService userService;

    public AdminController(BookService bookService, BorrowService borrowService, UserService userService) {
        this.bookService = bookService;
        this.borrowService = borrowService;
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("bookCount", bookService.search("", null).size());
        model.addAttribute("userCount", userService.findAll().size());
        model.addAttribute("activeBorrowCount", borrowService.countActiveBorrows());
        return "admin/dashboard";
    }

    @GetMapping("/admin/books")
    public String books(Model model) {
        model.addAttribute("books", bookService.search("", null));
        return "admin/books";
    }

    @GetMapping("/admin/books/new")
    public String newBook(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("categories", bookService.findCategories());
        model.addAttribute("mode", "create");
        return "admin/book-form";
    }

    @PostMapping("/admin/books")
    public String createBook(
        @Valid @ModelAttribute BookForm bookForm,
        BindingResult bindingResult,
        Model model,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            markFormInvalid(response);
            model.addAttribute("categories", bookService.findCategories());
            model.addAttribute("mode", "create");
            return "admin/book-form";
        }
        try {
            bookService.create(bookForm);
            redirectAttributes.addFlashAttribute("success", "Book created.");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException ex) {
            markFormInvalid(response);
            model.addAttribute("categories", bookService.findCategories());
            model.addAttribute("mode", "create");
            model.addAttribute("error", ex.getMessage());
            return "admin/book-form";
        }
    }

    @GetMapping("/admin/books/{id}/edit")
    public String editBook(@PathVariable Long id, Model model) {
        Book book = bookService.findBook(id);
        model.addAttribute("bookForm", bookService.toForm(book));
        model.addAttribute("book", book);
        model.addAttribute("bookId", id);
        model.addAttribute("categories", bookService.findCategories());
        model.addAttribute("mode", "edit");
        return "admin/book-form";
    }

    @PostMapping("/admin/books/{id}")
    public String updateBook(
        @PathVariable Long id,
        @Valid @ModelAttribute BookForm bookForm,
        BindingResult bindingResult,
        Model model,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            markFormInvalid(response);
            model.addAttribute("bookId", id);
            model.addAttribute("book", bookService.findBook(id));
            model.addAttribute("categories", bookService.findCategories());
            model.addAttribute("mode", "edit");
            return "admin/book-form";
        }
        try {
            bookService.update(id, bookForm);
            redirectAttributes.addFlashAttribute("success", "Book updated.");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException ex) {
            markFormInvalid(response);
            model.addAttribute("bookId", id);
            model.addAttribute("book", bookService.findBook(id));
            model.addAttribute("categories", bookService.findCategories());
            model.addAttribute("mode", "edit");
            model.addAttribute("error", ex.getMessage());
            return "admin/book-form";
        }
    }

    @PostMapping("/admin/books/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Book deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", userService.findAllRoles());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean enabled,
        @RequestParam(required = false) Set<String> roles,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            userService.updateUser(id, enabled, roles, principal.getName());
            redirectAttributes.addFlashAttribute("success", "User updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/borrows")
    public String borrows(Model model) {
        model.addAttribute("borrows", borrowService.findAll());
        return "admin/borrows";
    }

    @PostMapping("/admin/borrows/{id}/return")
    public String adminReturn(
        @PathVariable Long id,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            borrowService.returnBook(id, principal.getName(), true);
            redirectAttributes.addFlashAttribute("success", "Borrow returned.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/borrows";
    }

    private void markFormInvalid(HttpServletResponse response) {
        response.setStatus(422);
    }
}
