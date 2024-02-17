package ru.safronov.telegram.chatgptbot.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageDto {
    private final Long chatId;
    private final Integer messageId;
    private String text;
}