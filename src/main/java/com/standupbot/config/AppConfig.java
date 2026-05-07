package com.standupbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.standupbot.controller.ReportController;
import com.standupbot.controller.UpdateController;
import com.standupbot.repository.EntryRepository;
import com.standupbot.repository.InMemoryEntryRepository;
import com.standupbot.service.ClaudeReportService;
import com.standupbot.service.ReportService;
import com.standupbot.service.StubReportService;
import com.standupbot.service.StubTrelloService;
import com.standupbot.service.TrelloClient;
import com.standupbot.service.TrelloService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class AppConfig {

    public static Javalin buildApp() {
        EntryRepository repository = new InMemoryEntryRepository();

        String anthropicKey = System.getProperty("ANTHROPIC_API_KEY");
        String trelloKey    = System.getProperty("TRELLO_API_KEY");
        String trelloToken  = System.getProperty("TRELLO_TOKEN");
        String boardId      = System.getProperty("TRELLO_BOARD_ID");

        ReportService reportService;
        if (isPresent(anthropicKey) && isPresent(trelloKey) && isPresent(trelloToken) && isPresent(boardId)) {
            TrelloClient trelloClient = new TrelloClient(trelloKey, trelloToken, boardId);
            reportService = new ClaudeReportService(anthropicKey, trelloClient);
        } else {
            reportService = new StubReportService();
        }
        TrelloService trelloService = new StubTrelloService();

        UpdateController updateController = new UpdateController(repository);
        ReportController reportController = new ReportController(repository, reportService, trelloService);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, true)))
                .post("/update", updateController::submit)
                .get("/report", reportController::get);
    }

    private static boolean isPresent(String v) {
        return v != null && !v.isBlank();
    }
}
