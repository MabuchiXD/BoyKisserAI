package org.example.boykisserai;

import org.example.boykisserai.service.CharacterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

@SpringBootApplication
public class BoyKisserAiApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(BoyKisserAiApplication.class)
                .headless(false)
                .run(args);
    }

    @Bean
    public CommandLineRunner interactiveConsoleChatLoop(CharacterService characterService) {
        return args -> {
            // Запускаем консольный ввод в отдельном потоке
            Thread consoleThread = new Thread(() -> {
                try {
                    Thread.sleep(2000); // Ждем запуск WebSocket VTS и окна дашборда

                    Scanner scanner = new Scanner(System.in);

                    System.out.println("\n=======================================================");
                    System.out.println("  BOYKISSER AI — ПУЛЬТ УПРАВЛЕНИЯ И ГОЛОС АКТИВНЫ!");
                    System.out.println("  1. Открыто окно панели Создателя.");
                    System.out.println("  2. Микрофон и Discord активны.");
                    System.out.println("  3. Можно также писать прямо сюда в консоль.");
                    System.out.println("  (Чтобы выйти из консоли, напиши 'exit')");
                    System.out.println("=======================================================\n");

                    while (true) {
                        System.out.print("\nТы (Мабучи): ");
                        String userInput = scanner.nextLine().trim();

                        if ("exit".equalsIgnoreCase(userInput)) break;
                        if (userInput.isEmpty()) continue;

                        characterService.chat("mabuchi", "Мабучи", "LOCAL", userInput);
                    }
                } catch (Exception ignored) {}
            });

            consoleThread.setDaemon(true);
            consoleThread.start();
        };
    }
}