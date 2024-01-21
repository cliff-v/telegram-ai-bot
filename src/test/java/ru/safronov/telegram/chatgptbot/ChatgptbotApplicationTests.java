package ru.safronov.telegram.chatgptbot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChatgptbotApplicationTests {

	@Autowired
	BotComponent botComponent;

	@Test
	void contextLoads() {
		System.out.println(botComponent.getBotUsername());
	}

}
