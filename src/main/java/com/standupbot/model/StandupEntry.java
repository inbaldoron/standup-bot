package com.standupbot.model;

import java.time.Instant;

public record StandupEntry(String id, String summary, Instant submittedAt) {}
