package ru.safronov.telegram.bot.chatgpt;

public class Main {
    public static void main(String[] args) {
        while(true) {
            try {
                Thread.sleep(5500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Hello, world!");
        }
    }
}
