package com.example.summarizer.ports;

import java.time.OffsetDateTime;

public interface ClockPort {
    OffsetDateTime nowUtc();
}
