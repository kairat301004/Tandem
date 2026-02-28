package org.example.tandem.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    // NEWS
    NEWS_READ("news:read"),
    NEWS_CREATE("news:create"),
    NEWS_EDIT("news:edit"),
    NEWS_DELETE("news:delete"),

    // TASKS
    TASK_READ("task:read"),
    TASK_CREATE("task:create"),
    TASK_EDIT("task:edit"),
    TASK_DELETE("task:delete"),

    // USERS
    USER_READ("user:read"),
    USER_CREATE("user:create"),
    USER_EDIT("user:edit"),
    USER_DEACTIVATE("user:deactivate");

    private final String value;
}