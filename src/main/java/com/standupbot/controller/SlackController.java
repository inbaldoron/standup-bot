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
        log.info(">>> POST /slack/events called");
        try {
            String rawBody = ctx.body();
            log.info("Raw body: {}", rawBody);

            JsonNode body = mapper.readTree(rawBody);
            String type = body.path("type").asText();
            log.info("Event type: {}", type);

            if ("url_verification".equals(type)) {
                log.info("URL verification challenge received");
                ctx.result(body.path("challenge").asText());
                return;
            }

            ctx.status(HttpStatus.OK);

            if ("event_callback".equals(type)) {
                JsonNode event = body.path("event");
                String eventType = event.path("type").asText();
                String botId = event.path("bot_id").asText(null);
                log.info("event.type={} bot_id={} text={} channel={}",
                        eventType, botId, event.path("text").asText(), event.path("channel").asText());

                // Ignore messages sent by bots (including ourselves) to avoid loops
                if (botId != null) {
                    log.warn("DROPPING: message has bot_id={} — skipping to avoid loops", botId);
                    return;
                }

                if ("app_mention".equals(eventType) || "message".equals(eventType)) {
                    String channel = event.path("channel").asText();
                    String rawText = event.path("text").asText();
                    // Only respond to messages that actually mention the bot
                    if (!rawText.contains("<@")) {
                        log.warn("DROPPING: message has no @mention in text='{}'", rawText);
                        return;
                    }
                    String userMessage = rawText.replaceAll("<@[A-Z0-9]+>", "").trim();
                    log.info("Handling {}. channel={} userMessage={}", eventType, channel, userMessage);

                    String replyText = handleMessage(userMessage);
                    log.info("Reply text: {}", replyText);
                    postMessage(channel, replyText);
                } else {
                    log.info("Unhandled event type: {}", eventType);
                }
            } else {
                log.info("Unhandled top-level type: {}", type);
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
        log.info("Posting to channel={} token={}...{}", channel,
                botToken.substring(0, 10), botToken.substring(botToken.length() - 4));
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
