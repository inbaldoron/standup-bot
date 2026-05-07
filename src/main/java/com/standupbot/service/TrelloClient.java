package com.standupbot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

public class TrelloClient {

    private final String apiKey;
    private final String token;
    private final String boardId;
    private final HttpClient httpClient;

    public TrelloClient(String apiKey, String token, String boardId) {
        this.apiKey = apiKey;
        this.token = token;
        this.boardId = boardId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private String auth() {
        return "key=" + apiKey + "&token=" + token;
    }

    public String getBoard() throws IOException, InterruptedException {
        return get("/1/boards/" + boardId + "?" + auth());
    }

    public String getLists() throws IOException, InterruptedException {
        return get("/1/boards/" + boardId + "/lists?" + auth());
    }

    public String getCardsInList(String listId) throws IOException, InterruptedException {
        return get("/1/lists/" + listId + "/cards?" + auth());
    }

    public String getCardDetails(String cardId) throws IOException, InterruptedException {
        return get("/1/cards/" + cardId + "?fields=name,desc,due,dueComplete,idList,idMembers,labels,url&" + auth());
    }

    public String getRecentActions() throws IOException, InterruptedException {
        String since = LocalDate.now().minusDays(1).toString();
        return get("/1/boards/" + boardId + "/actions?filter=updateCard,createCard,commentCard&since=" + since + "&limit=50&" + auth());
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.trello.com" + path))
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("Trello " + res.statusCode());
        return res.body();
    }
}
