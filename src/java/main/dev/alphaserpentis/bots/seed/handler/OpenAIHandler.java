package dev.alphaserpentis.bots.seed.handler;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.ArrayList;

public class OpenAIHandler {
    public static OpenAiService service;
    public static final String model = "gpt-3.5-turbo";

    public static void init(@NonNull String apiKey) {
        service = new OpenAiService(apiKey);
    }

    public static boolean isPromptSafeToUse(@NonNull String prompt) {
        ModerationResult req = service.createModeration(
                new ModerationRequest(
                        prompt,
                        "text-moderation-stable"
                )
        );

        return !req.getResults().get(0).isFlagged();
    }

    public static ChatCompletionResult getCompletion(@NonNull String prompt) {
        ChatMessage message = new ChatMessage("user", prompt);
        ChatCompletionResult result = service.createChatCompletion(
                new ChatCompletionRequest(
                        model,
                        new ArrayList<>() {{
                            add(message);
                        }},
                        0.8,
                        null,
                        1,
                        false,
                        null,
                        null,
                        0.,
                        0.,
                        null,
                        "AMMY-POS-TEST"
                )
        );

        System.out.println(result.getUsage());

        return result;
    }
}
