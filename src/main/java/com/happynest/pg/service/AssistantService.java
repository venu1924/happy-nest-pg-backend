package com.happynest.pg.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final String SYSTEM = """
            You are the Happy Nest PG assistant. Answer the user's question using only the DATA.
            Rules:
            - If the DATA does not contain the answer, say you do not have that information.
            - Do not invent residents, rooms, payments, or amounts.
            - Be short and direct. Use names, room numbers, and amounts from the DATA.
            - Rent status PENDING means not paid yet. PAID means paid.
            - Deposit/advance is the amount recorded. Notes may say whether advance was paid.
            - Vacant beds are beds with status VACANT. Temporary occupants are not monthly residents.
            - Vacancies that ignore temporary stays are beds that are VACANT or occupied by TEMPORARY.
            """;

    private final ChatClient chatClient;
    private final PgSnapshotService snapshotService;
    private final String apiKey;

    public AssistantService(
            ChatClient.Builder chatClientBuilder,
            PgSnapshotService snapshotService,
            @Value("${app.ai.api-key:not-set}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.snapshotService = snapshotService;
        this.apiKey = apiKey;
    }

    public String ask(String question) {
        if (apiKey == null || apiKey.isBlank() || "not-set".equals(apiKey)) {
            return "The assistant is not switched on yet. Add AI_API_KEY on the Render service, then redeploy.";
        }
        String data = snapshotService.build();
        try {
            String answer = chatClient.prompt()
                    .system(SYSTEM)
                    .user("DATA:\n" + data + "\n\nQUESTION:\n" + question.trim())
                    .call()
                    .content();
            if (answer == null || answer.isBlank()) {
                return "I could not form an answer from the current data.";
            }
            return answer.trim();
        } catch (RuntimeException ex) {
            return "The assistant could not reply just now. Check the AI key and try again.";
        }
    }
}
