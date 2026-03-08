package com.community.community.comment;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {

    public record CommentCreateRequest(
            String content) {
    }

    public record CommentResponse(
            Long id,
            String content,
            String writer,
            LocalDateTime createdAt,
            List<CommentResponse> replies
    ) {
        public static CommentResponse from(CommentEntity comment) {
            return new CommentResponse(
                    comment.getId(),
                    // 💡 마스킹 처리: 삭제된 댓글이면 내용을 덮어씌움
                    comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                    // 💡 작성자 이름도 숨기고 싶다면 아래처럼 처리
                    comment.isDeleted() ? "(알 수 없음)" : comment.getUserEntity().getUsername(),
                    comment.getCreatedAt(),
                    comment.getChildren().stream()
                            .map(CommentResponse::from)
                            .toList()
            );
        }
    }

    public record CommentUpdateRequest(
            String content
    ) {
    }
}
