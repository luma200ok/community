package com.community.community.comment;

import java.time.LocalDateTime;

public class CommentDto {

    public record CommentCreateRequest(
            String content) {
    }

    public record CommentResponse(
            Long id,
            String content,
            String writer,
            LocalDateTime createdAt
    ) {
        public static CommentResponse from(CommentEntity comment) {
            return new CommentResponse(
                    comment.getId(),
                    comment.getContent(),
                    comment.getUserEntity().getUsername(),
                    comment.getCreatedAt()
            );
        }
    }

    public record CommentUpdateRequest(
            String content
    ) {
    }

}
