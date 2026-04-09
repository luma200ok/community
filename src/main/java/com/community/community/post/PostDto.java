package com.community.community.post;

import com.community.community.comment.CommentDto;
import com.community.community.comment.CommentEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

import static com.community.community.comment.CommentDto.*;

public class PostDto {

    public record PostCreateRequest(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            @Size(max = 10000, message = "내용은 10000자 이내여야 합니다.")
            String content,

            @NotBlank(message = "카테고리는 필수입니다.")
            String category
    ) {
    }

    public record PostDetailResponse(
            Long id,
            String title,
            String content,
            String writer,
            Long viewCount,
            Long likeCount,
            boolean isLiked,
            String category,
            // 1. 단일 -> List imageUrls로 변경
            List<String> imageUrls,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime createdAt,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime updatedAt,
            List<CommentResponse> comments // 댓글 리스트
    ) {
        public static PostDetailResponse from(
                PostEntity post, List<CommentEntity> comments, boolean isLiked, long totalViewCount) {
            // 2. PostEntity에 매달린 PostImageEntity들에서 URL만 쏙쏙 뽑아내어 리스트로
            List<String> urls = post.getImages().stream()
                    .map(PostImageEntity::getImageUrl)
                    .toList();

            return new PostDetailResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent(),
                    post.getUserEntity().getUsername(), // N+1 터지는 지점
                    totalViewCount,
                    post.getLikeCount(),
                    isLiked,
                    post.getCategory(),
                    urls, // 3. 뽑아낸 리스트 삽입
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    comments.stream()
                            .filter(c -> c.getParent() == null)
                            .map(CommentResponse::from)
                            .toList()
            );
        }
    }

    // 단건 조회
    public record PostListResponse(
            Long id,
            String title,
            String writer,
            Long viewCount,
            Long likeCount,
            String category,
            String thumbnailUrl,
            Long commentCount,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime createdAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
            LocalDateTime updatedAt
    ) {
        public static PostListResponse from(PostEntity post) {
            String thumb = (post.getImages() != null && !post.getImages().isEmpty())
                    ? post.getImages().get(0).getImageUrl()
                    : null;

            return new PostListResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getUserEntity().getUsername(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCategory(),
                    thumb,
                    post.getCommentCount(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }

    public record PostUpdateRequest(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
            String title,

            @NotBlank(message = "내용은 필수입니다.")
            @Size(max = 10000, message = "내용은 10000자 이내여야 합니다.")
            String content,

            @NotBlank(message = "카테고리는 필수입니다.")
            String category
    ) {
    }
}
