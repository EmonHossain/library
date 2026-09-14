INSERT IGNORE INTO roles (id, name) VALUES
  (1, 'ADMIN'),
  (2, 'USER');

INSERT IGNORE INTO users (id, full_name, email, password, enabled) VALUES
  (1, 'Library Admin', 'admin@library.test', '$2a$10$lTzu7DGdgpzmR./SsnGw6utxRuhUhjH3.4VKCR/yDofMOlquzEMti', true),
  (2, 'Demo User', 'user@library.test', '$2a$10$llp8AtINc8ARmUo/UVWJ1em7LVI/d7C3Nho6AIK.Iad4PhcHyFlpa', true);

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
  (1, 1),
  (1, 2),
  (2, 2);

INSERT IGNORE INTO categories (id, name, description) VALUES
  (1, 'Programming', 'Software engineering, languages, frameworks, and architecture.'),
  (2, 'Database', 'Relational databases, SQL, data modeling, and persistence.'),
  (3, 'Design', 'Product design, usability, and visual systems.'),
  (4, 'Fiction', 'Novels and narrative literature.');

INSERT IGNORE INTO books (id, title, author, isbn, description, total_copies, available_copies, category_id, created_at) VALUES
  (1, 'Clean Code', 'Robert C. Martin', '9780132350884', 'Practical guidance for writing readable and maintainable code.', 4, 4, 1, CURRENT_TIMESTAMP),
  (2, 'Effective Java', 'Joshua Bloch', '9780134685991', 'Best practices and idioms for the Java platform.', 3, 3, 1, CURRENT_TIMESTAMP),
  (3, 'Spring in Action', 'Craig Walls', '9781617297571', 'A hands-on introduction to building applications with Spring.', 3, 3, 1, CURRENT_TIMESTAMP),
  (4, 'SQL Antipatterns', 'Bill Karwin', '9781934356555', 'Common database design mistakes and better alternatives.', 2, 2, 2, CURRENT_TIMESTAMP),
  (5, 'Designing Data-Intensive Applications', 'Martin Kleppmann', '9781449373320', 'Architecture patterns for reliable, scalable, and maintainable systems.', 2, 2, 2, CURRENT_TIMESTAMP),
  (6, 'The Design of Everyday Things', 'Don Norman', '9780465050659', 'A classic on user-centered product design.', 2, 2, 3, CURRENT_TIMESTAMP),
  (7, 'The Hobbit', 'J.R.R. Tolkien', '9780547928227', 'A fantasy adventure following Bilbo Baggins.', 5, 5, 4, CURRENT_TIMESTAMP);
