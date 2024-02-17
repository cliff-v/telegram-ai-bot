package ru.safronov.telegram.chatgptbot.openai;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.Getter;
import ru.safronov.telegram.chatgptbot.models.MessageDto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class OpenAiHelper {
    @Getter
    private final static Map<Long, List<ChatMessage>> usersMessagesMap;
    @Getter
    private final static Map<Long, MessageDto> usersEditMessageMap;

    static {
        usersMessagesMap = new ConcurrentHashMap<>();
        usersEditMessageMap = new ConcurrentHashMap<>();
    }
}