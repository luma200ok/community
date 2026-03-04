package com.community.community.post;

import com.community.community.comment.CommentDto;
import com.community.community.comment.CommentEntity;

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
            String imageUrl,
            String writer,
            Long viewCount,
            Long likeCount,
            LocalDateTime createdAt,
            List<CommentResponse> comments // 댓글 리스트
    ) {
        public static PostDetailResponse from(
                PostEntity post, List<CommentEntity> comments) {
            return new PostDetailResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getImageUrl(),
                    post.getUserEntity().getUsername(), // N+1 터지는 지점
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCreatedAt(),
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
            LocalDateTime createdAt
    ) {
        public static PostListResponse from(PostEntity post) {
            return new PostListResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getUserEntity().getUsername(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCreatedAt()
            );
        }
    }

    public record PostUpdateRequest(String title, String content) {
    }
}
