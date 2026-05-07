package com.standupbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.standupbot.controller.ReportController;
import com.standupbot.controller.SlackController;
import com.standupbot.controller.UpdateController;
import com.standupbot.repository.EntryRepository;
import com.standupbot.repository.InMemoryEntryRepository;
import com.standupbot.service.ReportService;
import com.standupbot.service.StubReportService;
import com.standupbot.service.StubTrelloService;
import com.standupbot.service.TrelloService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class AppConfig {

    public static Javalin buildApp() {
        EntryRepository repository = new InMemoryEntryRepository();
        ReportService reportService = new StubReportService();
        TrelloService trelloService = new StubTrelloService();

        UpdateController updateController = new UpdateController(repository);
        ReportController reportController = new ReportController(repository, reportService, trelloService);
        SlackController slackController = new SlackController(repository, reportService, trelloService);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, true)))
                .post("/update", updateController::submit)
                .get("/report", reportController::get)
                .post("/slack/events", slackController::events);
    }
}
