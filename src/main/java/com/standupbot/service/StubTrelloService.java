package com.standupbot.service;

import com.standupbot.model.TrelloActivity;

import java.util.List;

public class StubTrelloService implements TrelloService {

    @Override
    public List<TrelloActivity> recentActivity() {
        return List.of();
    }
}
