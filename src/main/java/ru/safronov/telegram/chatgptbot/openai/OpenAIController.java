package ru.safronov.telegram.chatgptbot.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OpenAIController {

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/openai")
    public String getOpenAIResponse(@RequestBody String prompt) {
        return openAIService.callOpenAI(prompt);
    }
}