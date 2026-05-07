package com.standupbot.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.standupbot.model.StandupEntry;
import com.standupbot.model.TrelloActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClaudeReportService implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeReportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ITERATIONS = 10;

    private static final String SYSTEM_PROMPT = """
            You are a standup assistant for an engineering team. Your job is to generate a
            structured daily standup report by combining team members' self-reported updates
            with data from their Trello board.

            You have tools to query the Trello board. Use them to enrich the report:
            - Start with get_recent_actions to understand yesterday's activity.
            - Fetch list names with get_lists so you can give meaningful context.
            - Fetch card details only when you need specifics (due dates, descriptions).
            - Do NOT fetch every card on the board — be targeted and efficient.
            - When matching Trello activity to a person, use the member IDs in action
              records and card idMembers fields. If you can't confidently attribute an
              action to someone, note it as general team activity.
            - After gathering enough context, write the final report.

            Report format:
            ## Daily Standup Report

            ### <person id>
            **Yesterday:** <what they did, enriched with Trello context>
            **Today:** <what they plan>
            **Blockers:** <blockers or "None">

            ---

            Keep it concise. If no standup entries were submitted, summarize Trello activity instead.
            """;

    private static final Tool TOOL_GET_BOARD = Tool.builder()
            .name("get_board")
            .description("Board name and metadata")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder().build())
                    .build())
            .build();

    private static final Tool TOOL_GET_LISTS = Tool.builder()
            .name("get_lists")
            .description("All lists (columns) on the board")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder().build())
                    .build())
            .build();

    private static final Tool TOOL_GET_CARDS_IN_LIST = Tool.builder()
            .name("get_cards_in_list")
            .description("All cards in a specific list")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("listId", JsonValue.from(Map.of("type", "string")))
                            .build())
                    .required(List.of("listId"))
                    .build())
            .build();

    private static final Tool TOOL_GET_CARD_DETAILS = Tool.builder()
            .name("get_card_details")
            .description("Full card details including due date, desc, labels")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder()
                            .putAdditionalProperty("cardId", JsonValue.from(Map.of("type", "string")))
                            .build())
                    .required(List.of("cardId"))
                    .build())
            .build();

    private static final Tool TOOL_GET_RECENT_ACTIONS = Tool.builder()
            .name("get_recent_actions")
            .description("Card activity (updates/creates/comments) from last 24h")
            .inputSchema(Tool.InputSchema.builder()
                    .properties(Tool.InputSchema.Properties.builder().build())
                    .build())
            .build();

    private final AnthropicClient claude;
    private final TrelloClient trello;

    public ClaudeReportService(String anthropicApiKey, TrelloClient trelloClient) {
        this.claude = AnthropicOkHttpClient.builder().apiKey(anthropicApiKey).build();
        this.trello = trelloClient;
    }

    @Override
    public String generate(List<StandupEntry> entries, List<TrelloActivity> activity) {
        List<MessageParam> messages = new ArrayList<>();
        messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(buildUserMessage(entries))
                .build());

        Message response = callClaude(messages);
        int iterations = 0;

        while (isToolUse(response) && iterations < MAX_ITERATIONS) {
            log.info("Iteration {}: Claude requested tool use", iterations + 1);

            // Append assistant turn to history
            List<ContentBlockParam> assistantContent = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                if (block.isText()) {
                    assistantContent.add(ContentBlockParam.ofText(
                            TextBlockParam.builder().text(block.asText().text()).build()));
                } else if (block.isToolUse()) {
                    assistantContent.add(ContentBlockParam.ofToolUse(block.asToolUse().toParam()));
                }
            }
            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(assistantContent)
                    .build());

            // Execute tools and collect results
            List<ContentBlockParam> toolResults = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                if (block.isToolUse()) {
                    ToolUseBlock tb = block.asToolUse();
                    log.info("Dispatching tool: {}", tb.name());
                    String result = dispatchTool(tb.name(), tb._input());
                    toolResults.add(ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                    .toolUseId(tb.id())
                                    .content(result)
                                    .build()));
                }
            }
            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(toolResults)
                    .build());

            response = callClaude(messages);
            iterations++;
        }

        return response.content().stream()
                .filter(ContentBlock::isText)
                .map(b -> b.asText().text())
                .collect(Collectors.joining("\n"));
    }

    private Message callClaude(List<MessageParam> messages) {
        return claude.messages().create(
                MessageCreateParams.builder()
                        .model("claude-opus-4-7")
                        .maxTokens(4096L)
                        .system(SYSTEM_PROMPT)
                        .addTool(TOOL_GET_BOARD)
                        .addTool(TOOL_GET_LISTS)
                        .addTool(TOOL_GET_CARDS_IN_LIST)
                        .addTool(TOOL_GET_CARD_DETAILS)
                        .addTool(TOOL_GET_RECENT_ACTIONS)
                        .messages(messages)
                        .build());
    }

    private boolean isToolUse(Message response) {
        return response.stopReason()
                .filter(StopReason.TOOL_USE::equals)
                .isPresent();
    }

    private String dispatchTool(String name, JsonValue input) {
        try {
            return switch (name) {
                case "get_board"          -> trello.getBoard();
                case "get_lists"          -> trello.getLists();
                case "get_recent_actions" -> trello.getRecentActions();
                case "get_cards_in_list"  -> trello.getCardsInList(extractField(input, "listId"));
                case "get_card_details"   -> trello.getCardDetails(extractField(input, "cardId"));
                default -> "{\"error\": \"Unknown tool: " + name + "\"}";
            };
        } catch (Exception e) {
            log.error("Tool dispatch failed for {}: {}", name, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String extractField(JsonValue input, String field) {
        try {
            String json = MAPPER.writeValueAsString(input);
            return MAPPER.readTree(json).get(field).asText();
        } catch (Exception e) {
            throw new RuntimeException("Cannot extract field " + field, e);
        }
    }

    private String buildUserMessage(List<StandupEntry> entries) {
        if (entries.isEmpty()) {
            return "No standup entries submitted. Fetch recent Trello activity and summarize what the team worked on.";
        }
        StringBuilder sb = new StringBuilder("Here are today's standup entries:\n\n");
        for (StandupEntry e : entries) {
            sb.append("**").append(e.id()).append("** (submitted ").append(e.submittedAt()).append("):\n")
              .append(e.summary()).append("\n\n");
        }
        sb.append("Fetch relevant Trello data to enrich these updates and generate the team standup report.");
        return sb.toString();
    }
}
