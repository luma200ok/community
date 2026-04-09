package com.community.community.post;

import com.community.community.comment.CommentEntity;
import com.community.community.comment.CommentRepository;
import com.community.community.common.S3Service;
import com.community.community.redis.RedisService;
import com.community.community.exception.CustomException;
import com.community.community.like.LikeRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.community.community.exception.ErrorCode.EDIT_ACCESS_DENIED;
import static com.community.community.exception.ErrorCode.POST_NOT_FOUND;
import static com.community.community.exception.ErrorCode.USER_NOT_FOUND;
import static com.community.community.post.PostDto.PostCreateRequest;
import static com.community.community.post.PostDto.PostDetailResponse;
import static com.community.community.post.PostDto.PostListResponse;
import static com.community.community.post.PostDto.PostUpdateRequest;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostImageRepository postImageRepository;
    private final LikeRepository likeRepository;
    private final RedisService redisService;

    private final S3Service s3Service;

    @Value("${app.policy.post-retention-days:30}")
    private int postRetentionDays;


    @Value("${app.policy.view-count-ttl-hours:24}")
    private int viewCountTtlHours;

    public Long writePost(PostCreateRequest request, List<MultipartFile> images, Long userId) {
        UserEntity findUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if ("공지".equals(request.category()) && !findUser.isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }

        PostEntity post = PostEntity.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .userEntity(findUser)
                .build();
        postRepository.save(post);

        uploadImages(images, post);
        return post.getId();
    }

    public PostDetailResponse getPost(Long id, String clientIp, Long userId) {
        PostEntity post = postRepository.findByIdWithUserAndImages(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        String redisKey = "view:post:" + id + ":ip:" + clientIp;
        String viewed = redisService.getValues(redisKey);

        if (viewed == null) {
            redisService.increment("viewCount::"+id);
            redisService.setValues(redisKey, "viewed", Duration.ofHours(viewCountTtlHours));
        }

        boolean isLiked = false;
        if (userId != null) {
            isLiked = likeRepository.existsByUserEntity_IdAndPostEntity_Id(userId, id);
        }

        List<CommentEntity> comments = commentRepository.findCommentsByPostIdWithUser(id);

        return PostDetailResponse.from(post, comments, isLiked);
    }

    public void updatePost(Long id, PostUpdateRequest request, List<MultipartFile> images, Long userId) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        if (!post.getUserEntity().getId().equals(userId)) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }

        // H-6: 권한 검증을 S3 업로드보다 먼저 수행 (불필요한 파일 업로드 방지)
        if ("공지".equals(request.category()) && !post.getUserEntity().isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }

        if (images != null && !images.isEmpty()) {
            post.getImages().forEach(image -> s3Service.deleteFile(image.getImageUrl()));
            post.getImages().clear();
            uploadImages(images, post);
        }

        post.update(request.title(), request.content(), request.category());
    }

    private void uploadImages(List<MultipartFile> images, PostEntity post) {
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String imageUrl = s3Service.uploadImage(image);
                if (imageUrl != null) {
                    PostImageEntity postImage = PostImageEntity.builder()
                            .imageUrl(imageUrl)
                            .post(post)
                            .build();
                    postImageRepository.save(postImage);
                }
            }
        }
    }

    public void deletePost(Long id, Long userId) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        UserEntity requestUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!post.getUserEntity().getId().equals(userId) && !requestUser.isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }
        post.softDelete();
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getAllPost(Pageable pageable, String keyword) {
        Page<PostEntity> posts = postRepository.findPostsWithNoticeOnTop(keyword, pageable);
        return posts.map(PostListResponse::from);
    }

    public void hardDeleteOldPosts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(postRetentionDays); // yaml 설정값 적용
        List<String> imageUrlsToDelete = postRepository.findImageUrlsByOldDeletedPosts(threshold);
        
        // 2-1. DB에서 부모(게시글)가 지워지기 전에 자식(댓글) 데이터 먼저 싹 지우기 (외래키 제약조건 방어)
        postRepository.deleteCommentsByOldDeletedPosts(threshold);
        // 2-2. DB에서 부모(게시글)가 지워지기 전에 자식(이미지) 데이터 먼저 싹 지우기
        postRepository.deleteImagesByOldDeletedPosts(threshold);
        
        // 3. DB에서 게시글 데이터 일괄 삭제
        int deletedPostCount = postRepository.deleteOldDeletedPosts(threshold);

        if (!imageUrlsToDelete.isEmpty()) {
            imageUrlsToDelete.forEach(s3Service::deleteFile);
        }

        if (deletedPostCount > 0) {
            log.info("🧹 [Data GC] {}일 경과 휴지통 게시글 {}개 및 S3 파일 {}장 영구 삭제 완료!", postRetentionDays, deletedPostCount, imageUrlsToDelete.size());
        }
    }

    @Transactional
    public void syncViewCountFromRedis() {
        java.util.Set<String> keys = redisService.getKeys("viewCount::*");

        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            Long postId = Long.parseLong(key.split("::")[1]);
            String viewCountStr = redisService.getValues(key);
            if (viewCountStr != null) {
                long viewCount = Long.parseLong(viewCountStr);
                postRepository.findById(postId).ifPresent(post -> post.addViewCount(viewCount));
                redisService.delete(key);
            }
        }
        log.info("📊 [Redis Sync] {}개의 게시글 조회수를 DB에 동기화 완료했습니다.", keys.size());
    }
}
