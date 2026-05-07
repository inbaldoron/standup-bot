package com.standupbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.standupbot.model.ReportResponse;
import com.standupbot.model.StandupEntry;
import com.standupbot.model.TrelloActivity;
import com.standupbot.repository.EntryRepository;
import com.standupbot.service.ReportService;
import com.standupbot.service.TrelloService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SlackController {

    private static final Logger log = LoggerFactory.getLogger(SlackController.class);

    private final EntryRepository repository;
    private final ReportService reportService;
    private final TrelloService trelloService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public SlackController(EntryRepository repository,
                           ReportService reportService,
                           TrelloService trelloService) {
        this.repository = repository;
        this.reportService = reportService;
        this.trelloService = trelloService;
    }

    public void events(Context ctx) {
        try {
            JsonNode body = mapper.readTree(ctx.body());
            String type = body.path("type").asText();

            // Slack sends this once to verify the endpoint URL
            if ("url_verification".equals(type)) {
                ctx.result(body.path("challenge").asText());
                return;
            }

            // Acknowledge immediately — Slack requires a 200 within 3 seconds
            ctx.status(HttpStatus.OK);

            if ("event_callback".equals(type)) {
                JsonNode event = body.path("event");
                String eventType = event.path("type").asText();

                // Respond to direct mentions: @BotName <message>
                if ("app_mention".equals(eventType)) {
                    String channel = event.path("channel").asText();
                    String rawText = event.path("text").asText();
                    // Strip the @mention prefix
                    String userMessage = rawText.replaceAll("<@[A-Z0-9]+>", "").trim();

                    String replyText = handleMessage(userMessage);
                    postMessage(channel, replyText);
                }
            }

        } catch (Exception e) {
            log.error("Error handling Slack event", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String handleMessage(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("report") || lower.contains("summary") || lower.contains("standup")) {
            List<StandupEntry> entries = repository.all();
            List<TrelloActivity> activity = trelloService.recentActivity();
            if (entries.isEmpty()) {
                return "No standup entries yet. Submit updates via `POST /update` first.";
            }
            return reportService.generate(entries, activity);
        }

        if (lower.contains("help")) {
            return """
                    *Standup Bot commands* (mention me + one of these):
                    • `report` — generate today's standup report
                    • `help` — show this message
                    """;
        }

        return "I didn't understand that. Try mentioning me with `report` or `help`.";
    }

    private void postMessage(String channel, String text) {
        String botToken = System.getProperty("SLACK_BOT_TOKEN",
                System.getenv("SLACK_BOT_TOKEN"));
        if (botToken == null || botToken.isBlank()) {
            log.warn("SLACK_BOT_TOKEN not set — cannot post reply");
            return;
        }
        try {
            String payload = mapper.writeValueAsString(Map.of("channel", channel, "text", text));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://slack.com/api/chat.postMessage"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + botToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Slack postMessage response: {}", resp.body());
        } catch (Exception e) {
            log.error("Failed to post Slack message", e);
        }
    }
}
