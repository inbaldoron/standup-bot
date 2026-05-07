package com.standupbot.model;

import java.time.Instant;

public record ReportResponse(String report, Instant generatedAt, int entryCount) {}
