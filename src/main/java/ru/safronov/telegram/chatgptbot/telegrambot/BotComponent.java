package ru.safronov.telegram.chatgptbot.telegrambot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.safronov.telegram.chatgptbot.openai.OpenAiMainService;
import ru.safronov.telegram.chatgptbot.repositories.UsersRepository;

@Service
@Slf4j
public class BotComponent extends TelegramLongPollingBot {
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    private final String botToken;
    private final String username;
    private final OpenAiMainService openAiMainService;
    private final UsersRepository usersRepository;
    private final BotService botService;

    public BotComponent(@Value("${telegram.bot.token}") String botToken, @Value("${telegram.bot.username}") String username, OpenAiMainService openAiMainService, UsersRepository usersRepository, BotService botService) throws TelegramApiException {
        super(botToken);
        this.botToken = botToken;
        this.username = username;
        this.openAiMainService = openAiMainService;
        this.usersRepository = usersRepository;
        this.botService = botService;
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        User user = update.getMessage().getFrom();
        String text = update.getMessage().getText();

        var userDao = usersRepository.findByTgId(user.getId()).orElse(null);

        if (userDao == null || !BotService.checkAccess(userDao)) {
            sendMessage(user.getId(), "Иди своей дорогой, странник. Входа нет тебе в сию обитель");
            return;
        }
        userDao = botService.fillAndUpdateUsername(user, userDao);

        openAiMainService.callStreamOpenAI(text, userDao);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    public void sendMessage(Long who, String what) {
        SendMessage sm = SendMessage.builder().chatId(who.toString()).text(what).parseMode("MarkdownV2").build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}