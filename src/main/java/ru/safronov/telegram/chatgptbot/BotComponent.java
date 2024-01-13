package ru.safronov.telegram.chatgptbot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotComponent extends TelegramLongPollingBot {
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    private final String botToken;

    public BotComponent(@Value("${telegram.bot.token}") String botToken) throws TelegramApiException {
        super(botToken);
        this.botToken = botToken;
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText());
    }

    @Override
    public String getBotUsername() {
        return "NevduplenyshVIP_bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}