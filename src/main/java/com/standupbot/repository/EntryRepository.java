package com.standupbot.repository;

import com.standupbot.model.StandupEntry;

import java.util.List;

public interface EntryRepository {
    StandupEntry add(String id, String summary);
    List<StandupEntry> all();
}
