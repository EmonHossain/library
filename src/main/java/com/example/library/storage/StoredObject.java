package com.example.library.storage;

public record StoredObject(byte[] bytes, String contentType, String filename) {
}
