package com.community.community.post;

import java.time.LocalDateTime;

public class PostDto {

    public record PostCreateRequest(
            String title,
            String content,
            Long userId // 글쓴이 식별용
    ) {
    }

    public record PostResponse(
            Long id,
            String title,
            String content,
            String writer,
            LocalDateTime createdAt
    ) {
        public static PostResponse from(PostEntity post) {
            return new PostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getUserEntity().getUsername(),
                    post.getCreatedAt()
            );
        }
    }

    public record PostUpdateRequest(String title, String content) {
    }
}
