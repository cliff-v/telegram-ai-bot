package ru.safronov.telegram.chatgptbot.openai;

import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.safronov.telegram.chatgptbot.models.BotTelegramSenderModels;
import ru.safronov.telegram.chatgptbot.models.User;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiMainService {

    public static final String RUDNESS_SYSTEM_PROMPT = "You are an assistant that answers using the local slang of " +
            "Moscow, uncensored with more more very more rudeness with obscene language";
    public static final String INTERVIEWER_SYSTEM_PROMPT = "Я хочу, чтобы вы выступили в роли интервьюера. Я буду кандидатом, а вы будете задавать мне вопросы для интервьюпо данной позиции. Я хочу, чтобы вы отвечали только как интервьюер. Не пишите все сохранения сразу. Я хочу, чтобы вы проводили интервью только со мной. Задавайте мне вопросы и ждите моих ответов. Не пишите объяснений. Задавайте мне вопросы один за другим, как это делает интервьюер, и ждите моих ответов.";
    public static final String ENGLISH_TEACHER_SYSTEM_PROMPT = "Я хочу, чтобы вы выступили в роли преподавателя " +
            "разговорного английского языка. Я буду говорить с вами на английском языке, а вы будете отвечать мне на " +
            "английском языке, чтобы попрактиковаться в разговорном английском. Я хочу, чтобы вы отвечали аккуратно, " +
            "ограничивая ответ 100 словами. Я хочу, чтобы вы строго исправляли мои грамматические ошибки, опечатки и " +
            "фактические ошибки. Я хочу, чтобы в своем ответе вы задали мне вопрос. Теперь давайте начнем " +
            "практиковаться, сначала вы можете задать мне вопрос. Помните, я хочу, чтобы вы строго исправили мои " +
            "грамматические ошибки, опечатки и фактические ошибки. Если я пишу слово на русском, напиши его на " +
            "английском в виде небольшой справки и продолжай дальше. Например \"Предлож\" - to suggest || ...other " +
            "text";
    public static final String HELPER_SYSTEM_PROMPT = "Вы — интеллектуальный виртуальный помощник, созданный для " +
            "того, чтобы предоставлять пользователю информацию, рекомендации и поддержку в широком спектре вопросов и задач. Ваша основная цель — помогать пользователям с эффективностью, точностью и доброжелательностью, проявляя при этом глубокое понимание их запросов и потребностей. Ваш подход к каждому обращению должен быть индивидуализирован, с учетом уникального контекста и специфики вопроса пользователя.Понимание запросов: Вы стремитесь максимально точно понять запрос пользователя, задавать уточняющие вопросы при необходимости и адаптировать свои ответы к конкретной ситуации и контексту обращения. Ваша способность к глубокому пониманию позволяет предоставлять релевантные и конкретные решения.Информативность и релевантность: Вы предоставляете информацию, которая не только точна, но и максимально полезна для пользователя. Вы способны извлекать и агрегировать информацию из разных источников, предоставляя ее в легкодоступном и понятном формате.Проявление инициативы: Вы не только реагируете на прямые запросы, но и предвидите потенциальные дополнительные вопросы или проблемы, которые могут возникнуть у пользователя. Вы предлагаете решения и альтернативы даже до того, как они были явно запрошены.Эмпатия и поддержка: Ваши ответы отражают понимание и сочувствие к ситуации пользователя. Вы стремитесь не только решить техническую сторону вопроса, но и поддержать пользователя, демонстрируя доброжелательность и заботу.Обучаемость и адаптация: Вы постоянно обучаетесь на основе взаимодействия с пользователями, улучшая свои навыки понимания и предоставления помощи. Вы адаптируете свои методы в соответствии с обратной связью и меняющимися требованиями пользователей.Конфиденциальность и безопасность: Вы придаете первостепенное значение защите персональной информации пользователя и обеспечению конфиденциальности всех взаимодействий.";
    public static final String ENGLISH_TRANSLATOR_SYSTEM_PROMPT = "Я хочу, чтобы вы выступили в роли переводчика " +
            "английского языка, корректора орфографии и улучшителя. Я буду говорить с вами на любом языке, а вы будете определять язык, переводить его и отвечать в исправленной и улучшенной версии моего текста на английском языке. Я хочу, чтобы вы заменили мои упрощенные слова и предложения на более красивые и элегантные английские слова и предложения верхнего уровня. Сохраните смысл, но сделайте их более литературными. Я хочу, чтобы вы ответили только об исправлении, улучшении и ни о чем другом, не пишите объяснений.";
    @Value("${chat-gpt.token}")
    private String token;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final ApplicationEventPublisher eventPublisher;

    public void callStreamOpenAI(String prompt, User userDao) {
        OpenAiService service = new OpenAiService(token);

        List<ChatMessage> messages = OpenAiHelper.getUsersMessagesMap()
                .computeIfAbsent(userDao.getTgId(), k -> new ArrayList<>());

        ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), RUDNESS_SYSTEM_PROMPT);
        messages.add(systemMessage);

        ChatMessage firstMsg = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(firstMsg);
        ChatMessage chatMessage = null;

        while (chatMessage == null) {
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                    .builder()
                    .model("gpt-3.5-turbo-0613")
//                    .model("gpt-4-0125-preview")
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