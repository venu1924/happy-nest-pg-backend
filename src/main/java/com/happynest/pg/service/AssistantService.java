package com.happynest.pg.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AssistantService {

    private static final String SYSTEM = """
            You are the Happy Nest PG assistant. Answer any question that can be answered from the DATA.
            This includes residents, rooms, beds, floors, rent, who paid, deposits, advance, refunds,
            temporary stays, vacancies, past residents, and transfers.
            Rules:
            - Use only the DATA. Do not invent people, rooms, payments, or amounts.
            - If the DATA does not contain what is needed, reply with exactly: Data is unavailable.
            - Be short and direct. Use the names, room numbers, and amounts from the DATA.
            - Rent status PENDING means not paid yet. PAID means paid.
            - Deposit/advance is the amount recorded. Notes may say whether advance was paid.
            - A bed is vacant when its status is VACANT.
            - Vacancies ignoring temporary stays are beds that are VACANT or occupied by TEMPORARY.
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

    public Flux<String> stream(String question) {
        if (apiKey == null || apiKey.isBlank() || "not-set".equals(apiKey)) {
            return Flux.just("The assistant is not switched on yet. Add AI_API_KEY on the Render service, then redeploy.");
        }
        final String data;
        try {
            data = snapshotService.build();
        } catch (RuntimeException ex) {
            return Flux.just("Data is unavailable.");
        }
        return chatClient.prompt()
                .system(SYSTEM)
                .user("DATA:\n" + data + "\n\nQUESTION:\n" + question.trim())
                .stream()
                .content()
                .filter(token -> token != null && !token.isEmpty())
                .switchIfEmpty(Flux.just("Data is unavailable."))
                .onErrorResume(ex -> Flux.just("The assistant could not reply just now. Check the AI key and try again."));
    }
}
