package ru.safronov.telegram.chatgptbot.telegram_bot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.safronov.telegram.chatgptbot.models.BotTelegramSenderModels;
import ru.safronov.telegram.chatgptbot.models.MessageDto;
import ru.safronov.telegram.chatgptbot.openai.OpenAiHelper;
import ru.safronov.telegram.chatgptbot.openai.OpenAiMainService;
import ru.safronov.telegram.chatgptbot.repositories.UsersRepository;

import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class BotComponent extends TelegramLongPollingBot {
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    private final String botToken;
    private final String username;
    private final OpenAiMainService openAiMainService;
    private final UsersRepository usersRepository;

    public BotComponent(@Value("${telegram.bot.token}") String botToken, @Value("${telegram.bot.username}") String username, OpenAiMainService openAiMainService, UsersRepository usersRepository) throws TelegramApiException {
        super(botToken);
        this.botToken = botToken;
        this.username = username;
        this.openAiMainService = openAiMainService;
        this.usersRepository = usersRepository;
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
        if (userDao == null || !(userDao.getType().equals("ADMIN") || userDao.getType().equals("USER"))) {
            return;
        }

        openAiMainService.callStreamOpenAI(text, userDao);
    }

    @Override
    public String getBotUsername() {
        return username;
    }


    @EventListener
    public void handleSendMessage(BotTelegramSenderModels.SendMessageEvent event) {
        SendMessage sm = SendMessage.builder().chatId(event.getChatId().toString())
                .text(event.getMessage().isBlank() ? "." : event.getMessage())
                .build();

        Message message;
        try {
           message = execute(sm);
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

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        event.getCdl().countDown();
    }
}