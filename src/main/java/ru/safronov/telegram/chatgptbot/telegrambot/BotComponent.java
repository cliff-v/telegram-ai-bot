package ru.safronov.telegram.chatgptbot.telegrambot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
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
        if (userDao == null || !checkAccess(userDao)) {
            sendMessage(user.getId(), "Иди своей дорогой, странник. Нет тебе входа в сию обитель");
            return;
        }

        openAiMainService.callStreamOpenAI(text, userDao);
    }

    private static boolean checkAccess(ru.safronov.telegram.chatgptbot.models.User userDao) {
        return userDao.getType().equals("ADMIN") || userDao.getType().equals("USER");
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    public void sendMessage(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what)
                .parseMode("MarkdownV2")
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener
    public void handleSendMessage(BotTelegramSenderModels.SendMessageEvent event) {
        SendMessage sm = SendMessage.builder().chatId(event.getChatId().toString())
                .text(event.getMessage().isBlank() ? "." : event.getMessage())
                .parseMode("Markdown")
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
        editMessageText.setParseMode("Markdown");

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        event.getCdl().countDown();
    }
}