package com.example.summarizer.ports;

public interface SummarizerPort {
    String generate(String prompt) throws Exception;
}
