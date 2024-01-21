package ru.safronov.telegram.chatgptbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatgptbotApplication {
	public static void main(String[] args) {
		System.getenv().forEach((k, v) -> System.out.println(k + " :kotik: " + v));
		SpringApplication.run(ChatgptbotApplication.class, args);
	}
}
