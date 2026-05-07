package com.standupbot.service;

import com.standupbot.model.StandupEntry;
import com.standupbot.model.TrelloActivity;

import java.util.List;

public interface ReportService {
    String generate(List<StandupEntry> entries, List<TrelloActivity> activity);
}
