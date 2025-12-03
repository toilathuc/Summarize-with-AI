package com.example.summarizer.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ContentHashUtils {
    private ContentHashUtils() {};

    public static String contentHash(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean isContentHashMatch(String oldContent, String newContent) {
        String oldHash = contentHash(oldContent);
        String newHash = contentHash(newContent);
        if (oldHash == null || newHash == null) {
            return false;
        }
        return oldHash.equals(newHash);
    };
}
