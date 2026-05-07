package com.standupbot.controller;

import com.standupbot.model.ReportResponse;
import com.standupbot.model.StandupEntry;
import com.standupbot.model.TrelloActivity;
import com.standupbot.repository.EntryRepository;
import com.standupbot.service.ReportService;
import com.standupbot.service.TrelloService;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.List;

public class ReportController {

    private final EntryRepository repository;
    private final ReportService reportService;
    private final TrelloService trelloService;

    public ReportController(EntryRepository repository,
                            ReportService reportService,
                            TrelloService trelloService) {
        this.repository = repository;
        this.reportService = reportService;
        this.trelloService = trelloService;
    }

    public void get(Context ctx) {
        List<StandupEntry> entries = repository.all();
        List<TrelloActivity> activity = trelloService.recentActivity();
        String report = reportService.generate(entries, activity);
        ctx.json(new ReportResponse(report, Instant.now(), entries.size()));
    }
}
