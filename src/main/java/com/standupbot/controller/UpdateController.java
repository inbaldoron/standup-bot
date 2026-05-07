package com.standupbot.controller;

import com.standupbot.model.StandupEntry;
import com.standupbot.model.UpdateRequest;
import com.standupbot.repository.EntryRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;

public class UpdateController {

    private final EntryRepository repository;

    public UpdateController(EntryRepository repository) {
        this.repository = repository;
    }

    public void submit(Context ctx) {
        UpdateRequest req = ctx.bodyAsClass(UpdateRequest.class);
        if (req == null || isBlank(req.id()) || isBlank(req.summary())) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(Map.of("error", "id and summary are required"));
            return;
        }
        StandupEntry stored = repository.add(req.id().trim(), req.summary().trim());
        ctx.status(HttpStatus.CREATED).json(stored);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
