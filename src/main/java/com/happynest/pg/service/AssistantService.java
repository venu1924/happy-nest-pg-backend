package com.happynest.pg.service;

import com.happynest.pg.dto.AskRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AssistantService {

    private static final String SYSTEM = """
            You are the Happy Nest PG assistant. You can chat normally and you can answer from the DATA.
            This includes residents, rooms, beds, floors, rent, who paid, deposits, advance, refunds,
            temporary stays, vacancies, past residents, and transfers.
            Rules:
            - For greetings and small talk such as hi, hello, hey, wassup, thanks, or bye, reply in a warm short way.
              Do not say "Data is unavailable" for those. You may offer to share today's PG update.
            - If they ask for today's updates, the status, a summary, what is happening, how the PG is,
              or any other broad question, cover the whole picture in TODAY AT THE PG.
              Include monthly residents, who has not paid and who has, whether everyone is paid,
              temporary stays, who is unpaid, who should vacate today, vacant beds, empty rooms,
              rooms with a free bed, vacancies by floor, transfers today, and anyone who left today.
              If a part is zero or none, say that plainly. Do not stop after only one of these.
            - For a specific question, answer that question from the DATA.
            - Use only the DATA for PG facts. Do not invent people, rooms, payments, or amounts.
            - If a specific fact is not in the DATA, reply with exactly: Data is unavailable.
            - Use the recent chat when the new message refers to it.
            - Be direct. Use names, room numbers, and amounts from the DATA.
            - Rent status PENDING means not paid yet. PAID means paid.
            - Deposit/advance is the amount recorded. Notes may say whether advance was paid.
            - A bed is vacant when its status is VACANT.
            - Vacancies ignoring temporary stays are beds that are VACANT or occupied by TEMPORARY.
            - Reply with the answer only. Do not describe your reasoning.
            """;

    private static final String MODEL = "openai/gpt-oss-20b";

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

    public Flux<String> stream(String question, List<AskRequest.ChatLine> history) {
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
                .options(OpenAiChatOptions.builder()
                        .model(MODEL)
                        .temperature(0.2)
                        .maxTokens(1600)
                        .build())
                .system(SYSTEM)
                .user("DATA:\n" + data + "\n\nRECENT CHAT:\n" + formatHistory(history)
                        + "\nQUESTION:\n" + question.trim())
                .stream()
                .content()
                .filter(token -> token != null && !token.isEmpty())
                .switchIfEmpty(Flux.just("Data is unavailable."))
                .onErrorResume(ex -> Flux.just("The assistant could not reply just now. Check the AI key and try again."));
    }

    private static String formatHistory(List<AskRequest.ChatLine> history) {
        if (history == null || history.isEmpty()) {
            return "none\n";
        }
        StringBuilder text = new StringBuilder();
        int start = Math.max(0, history.size() - 6);
        for (AskRequest.ChatLine line : history.subList(start, history.size())) {
            if (line == null || line.getText() == null || line.getText().isBlank()) {
                continue;
            }
            String role = "ai".equalsIgnoreCase(line.getRole()) ? "Assistant" : "Owner";
            String message = line.getText().replace('\n', ' ').trim();
            if (message.length() > 400) {
                message = message.substring(0, 400);
            }
            text.append(role).append(": ").append(message).append('\n');
        }
        return text.isEmpty() ? "none\n" : text.toString();
    }
}
