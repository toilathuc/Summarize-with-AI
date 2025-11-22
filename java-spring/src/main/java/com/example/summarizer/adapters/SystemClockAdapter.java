package com.example.summarizer.adapters;

import com.example.summarizer.ports.ClockPort;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class SystemClockAdapter implements ClockPort {
    @Override
    public OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
