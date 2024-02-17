package ru.safronov.telegram.chatgptbot.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OpenAIController {

    @Autowired
    private OpenAiMainService openAiMainService;

    @PostMapping("/openai")
    public String getOpenAIResponse(@RequestBody String prompt) {
        return "openAiMainService.callOpenAI(prompt)";
    }
}