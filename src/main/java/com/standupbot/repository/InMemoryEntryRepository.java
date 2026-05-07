package com.standupbot.repository;

import com.standupbot.model.StandupEntry;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEntryRepository implements EntryRepository {

    private final List<StandupEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public StandupEntry add(String id, String summary) {
        StandupEntry entry = new StandupEntry(id, summary, Instant.now());
        entries.add(entry);
        return entry;
    }

    @Override
    public List<StandupEntry> all() {
        return List.copyOf(entries);
    }
}
