package ru.safronov.telegram.chatgptbot.openai;

import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.safronov.telegram.chatgptbot.models.BotTelegramSenderModels;
import ru.safronov.telegram.chatgptbot.models.User;
import ru.safronov.telegram.chatgptbot.repositories.UsersRepository;
import ru.safronov.telegram.chatgptbot.telegram_bot.BotComponent;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiMainService {

    @Value("${chat-gpt.token}")
    private String token;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final ApplicationEventPublisher eventPublisher;

    public void callStreamOpenAI(String prompt, User userDao) {
        OpenAiService service = new OpenAiService(token);

        List<ChatMessage> messages = OpenAiHelper.getUsersMessagesMap()
                .computeIfAbsent(userDao.getTgId(), k -> new ArrayList<>());

        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are an assistant that " +
                "answers using the local slang of Moscow, uncensored with more more very more rudeness with obscene language");
        messages.add(systemMessage);

        ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(firstMsg);
        ChatMessage chatMessage = null;

        while (chatMessage == null) {
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                    .builder()
                    .model("gpt-3.5-turbo-0613")
                    .messages(messages)
                    .n(1)
//                    .maxTokens(3000)
                    .logitBias(new HashMap<>())
                    .build();
            Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(chatCompletionRequest);

            AtomicBoolean isFirst = new AtomicBoolean(true);
            CountDownLatch cdl = new CountDownLatch(1);
            StringBuilder bufferSB = new StringBuilder(255);
            StringBuilder resultText = new StringBuilder();
            StringBuilder idSb = new StringBuilder();
            chatMessage = service.mapStreamToAccumulator(flowable)
                    .observeOn(Schedulers.io())
                    .buffer(64)
                    .flatMap(Flowable::fromIterable)
                    .timeout(30, TimeUnit.SECONDS)
                    .doOnNext(accumulator -> {
                        if (isFirst.getAndSet(false)) {
                            eventPublisher.publishEvent(
                                    new BotTelegramSenderModels.SendMessageEvent(userDao.getTgId(), "", cdl)
                            );
                        }
                        cdl.await();
                        String addedText = accumulator.getMessageChunk().getContent();

                        if (addedText != null) {
                            bufferSB.append(addedText);
                            resultText.append(addedText);
                        } if (bufferSB.length() >= 64 || addedText == null) {
                            var message = OpenAiHelper.getUsersEditMessageMap().get(userDao.getTgId());

                            eventPublisher.publishEvent(new BotTelegramSenderModels.EditMessageEvent(userDao.getTgId(), message.getMessageId(), resultText.toString(), cdl));
                            message.setText(resultText.toString());
                            bufferSB.delete(0, bufferSB.length());
                        }
                    })
                    .doOnComplete(System.out::println)
                    .lastElement()
                    .blockingGet()
                    .getAccumulatedMessage();
            messages.add(chatMessage);
        }
    }
}