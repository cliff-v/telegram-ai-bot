package ru.safronov.telegram.chatgptbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatgptbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatgptbotApplication.class, args);
		System.out.println("Start!");
	}

}
