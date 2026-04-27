package com.community.community.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DiscordNotificationService {

    private final String webhookUrl;
    private final RestClient restClient;

    public DiscordNotificationService(@Value("${discord.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
    }

    @Async
    public void sendSignupNotification(String username) {
        sendEmbed("🎉 새 회원 가입", username + " 님이 가입하셨습니다!", 0x57F287);
    }

    @Async
    public void sendPostNotification(String username, String title, String category) {
        String description = "**[" + category + "]** " + title + "\n작성자: " + username;
        sendEmbed("📝 새 게시글", description, 0x5865F2);
    }

    private void sendEmbed(String title, String description, int color) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> embed = Map.of(
                    "title", title,
                    "description", description,
                    "color", color
            );
            Map<String, Object> body = Map.of("embeds", List.of(embed));

            restClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[Discord] 알림 전송 실패: {}", e.getMessage());
        }
    }
}
