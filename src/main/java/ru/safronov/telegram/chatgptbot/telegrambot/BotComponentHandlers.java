package ru.safronov.telegram.chatgptbot.telegrambot;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.safronov.telegram.chatgptbot.models.BotTelegramSenderModels;
import ru.safronov.telegram.chatgptbot.models.MessageDto;
import ru.safronov.telegram.chatgptbot.openai.OpenAiHelper;

@Component
@RequiredArgsConstructor
public class BotComponentHandlers {
    private final BotComponent botComponent;
    @EventListener
    public void handleSendMessage(BotTelegramSenderModels.SendMessageEvent event) {
        SendMessage sm = SendMessage.builder().chatId(event.getChatId().toString())
                .text(event.getMessage().isBlank() ? "." : event.getMessage())
                .parseMode("Markdown")
                .build();

        Message message;
        try {
            message = botComponent.execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        OpenAiHelper.getUsersEditMessageMap().put(event.getChatId(), MessageDto.builder()
                .chatId(event.getChatId())
                .messageId(message.getMessageId())
                .text(event.getMessage())
                .build()
        );
        event.getCdl().countDown();
    }

    @EventListener
    public void handleEditMessage(BotTelegramSenderModels.EditMessageEvent event) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(String.valueOf(event.getChatId()));
        editMessageText.setMessageId(event.getMessageId());
        editMessageText.setText(event.getNewText());
        editMessageText.setParseMode("Markdown");

        try {
            botComponent.execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        event.getCdl().countDown();
    }
}
