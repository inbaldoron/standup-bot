package com.standupbot.service;

import com.standupbot.model.StandupEntry;
import com.standupbot.model.TrelloActivity;

import java.util.List;
import java.util.stream.Collectors;

public class StubReportService implements ReportService {

    @Override
    public String generate(List<StandupEntry> entries, List<TrelloActivity> activity) {
        if (entries.isEmpty()) {
            return "# Standup Report\n\n_No standup entries yet._\n";
        }
        String body = entries.stream()
                .map(e -> "## " + e.id() + "\n" + e.summary() + "\n")
                .collect(Collectors.joining("\n"));
        return "# Standup Report\n\n" + body;
    }
}
