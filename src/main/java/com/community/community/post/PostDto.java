package com.community.community.post;

import com.community.community.comment.CommentDto;
import com.community.community.comment.CommentEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

import static com.community.community.comment.CommentDto.*;

public class PostDto {

    public record PostCreateRequest(
            String title,
            String content
//            Long userId // 글쓴이 식별용 -> JWT 필터로 user 확인
    ) {
    }

    public record PostDetailResponse(
            Long id,
            String title,
            String content,
            String writer,
            Long viewCount,
            Long likeCount,
            // 1. 단일 -> List imageUrls로 변경
            List<String> imageUrls,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime createdAt,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime updatedAt,
            List<CommentResponse> comments // 댓글 리스트
    ) {
        public static PostDetailResponse from(
                PostEntity post, List<CommentEntity> comments) {
            // 2. PostEntity에 매달린 PostImageEntity들에서 URL만 쏙쏙 뽑아내어 리스트로
            List<String> urls = post.getImages().stream()
                    .map(PostImageEntity::getImageUrl)
                    .toList();

            return new PostDetailResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getUserEntity().getUsername(), // N+1 터지는 지점
                    post.getViewCount(),
                    post.getLikeCount(),
                    urls, // 3. 뽑아낸 리스트 삽입
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    comments.stream().map(CommentResponse::from).toList()
            );
        }
    }

    public record PostListResponse(
            Long id,
            String title,
            String writer,
            Long viewCount,
            Long likeCount,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime createdAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime updatedAt
    ) {
        public static PostListResponse from(PostEntity post) {
            return new PostListResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getUserEntity().getUsername(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }

    public record PostUpdateRequest(String title, String content) {
    }
}
