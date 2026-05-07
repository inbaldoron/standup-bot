package com.standupbot.service;

import com.standupbot.model.TrelloActivity;

import java.util.List;

public interface TrelloService {
    List<TrelloActivity> recentActivity();
}
