package com.community.community.like;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "❤️ 좋아요 API", description = "특정 게시글에 대한 좋아요 토글(등록 및 취소) 기능을 제공합니다.")@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/likes")
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "좋아요 등록 및 취소",
            description = "특정 게시글에 좋아요를 누릅니다. 이미 좋아요를 누른 상태라면 취소(삭제)됩니다.\n\n" +
                    "**권한:** **JWT 토큰이 필수**입니다.")
    @PostMapping
    public ResponseEntity<String> toggleLike(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        boolean isLiked = likeService.toggleLike(postId, userId);

        String message = isLiked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(message);
    }
}
