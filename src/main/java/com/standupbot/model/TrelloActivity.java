package com.standupbot.model;

import java.time.Instant;

public record TrelloActivity(String cardId, String cardName, String action, Instant when) {}
