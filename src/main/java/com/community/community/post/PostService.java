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

    // ==========================================
    // 💡 [추가] yaml 설정 파일에서 정책 값 가져오기
    // ==========================================
    @Value("${app.policy.post-retention-days:30}") // (설정이 없으면 기본값 30 적용)
    private int postRetentionDays;

    @Value("${app.policy.view-count-ttl-hours:24}")
    private int viewCountTtlHours;

    /**
     * 게시글 작성 (다중 이미지 버전)
     */
    public Long writePost(PostCreateRequest request, List<MultipartFile> images, Long userId) {
        // 1. 작성자 조회
        UserEntity findUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 🚨 일반 유저가 몰래 '공지' 카테고리로 글을 쓰려 하면 에러 발생!
        if ("공지".equals(request.category()) && !findUser.isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED); // 권한 없음 에러
        }

        // 2. 게시글(Post) 먼저 생성 및 DB에 저장 (사진은 아직 업로드X)
        PostEntity post = PostEntity.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category()) // 카테고리 저장
                .userEntity(findUser) // 유저 객체 맵핑
                .build();
        postRepository.save(post);

        // 3. 첨부된 이미지들이 있다면 for문으로 하나씩 처리
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String imageUrl = s3Service.uploadImage(image);

                if (imageUrl != null) {
                    // S3 주소와 방금 만든 게시글(Post)을 엮어 사진 DB에 저장
                    PostImageEntity postImage = PostImageEntity.builder()
                            .imageUrl(imageUrl)
                            .post(post)
                            .build();
                    postImageRepository.save(postImage);
                }
            }
        }
        return post.getId();
    }

    /**
     * 게시글 단건 조회 (Id)
     * Transactional readOnly -> 조회수 증가 로직 때문에 주석처리
     */
//    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long id, String clientIp, Long userId) {
        // 1. 글 번호를 통해 DB에서 게시글 조회. 없으면 에러
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        // 2. 조회수 어뷰징 방지 로직 (Redis 활용)
        // 레디스에 저장할 키 이름 생성 ("view:post:1:ip:127.0.0.1")
        String redisKey = "view:post:" + id + ":ip:" + clientIp;
        // 레디스 금고에 해당 키가 존재하는지 확인
        String viewed = redisService.getValues(redisKey);

        if (viewed == null) {
            // 금고에 없다 최초 조회
            post.increaseViewCount();

            redisService.setValues(redisKey, "viewed", Duration.ofHours(viewCountTtlHours));
        }

        // 이 유저가 좋아요를 눌렀는지 검증
        boolean isLiked = false;
        if (userId != null) {
            isLiked = likeRepository.existsByUserEntity_IdAndPostEntity_Id(userId, id);
        }

        // 3. 게시글에 달린 댓글 가져오기
        List<CommentEntity> comments = commentRepository.findByPostEntityId(id);

        // 4. 찾은 Entity를 DTO로 변환해서 반환
        return PostDetailResponse.from(post, comments, isLiked);
    }

    /**
     * 게시글 ID를 통한 수정
     * 작성자와 수정 요청자 userId 검증
     */
    public void updatePost(Long id, PostUpdateRequest request, List<MultipartFile> images, Long userId) {
        // 1. 수정할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        // 2. 작성자와 수정 요청자 일치 검증
        if (!post.getUserEntity().getId().equals(userId)) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }

        // 3. 이미지 교체 로직
        // 새로운 사진 파일이 들어온 경우에만 실행
        if (images != null && !images.isEmpty()) {
            // 3-1. 기존 S3 파일 전체 삭제
            for (PostImageEntity oldImage : post.getImages()) {
                s3Service.deleteFile(oldImage.getImageUrl());
            }
            // 3-2. DB에서 기존 파일 데이터 지우기
            // PostEntity에 orphanRemoval = true가 켜져 사용 시 필요 없음.
            // postImageRepository.deleteByPostId(post.getId());
            post.getImages().clear();
            // 3-3. 새 파일을 S3에 업로드하고 DB에 저장
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String newImageUrl = s3Service.uploadImage(image);
                    if (newImageUrl != null) {
                        PostImageEntity postImage = PostImageEntity.builder()
                                .imageUrl(newImageUrl)
                                .post(post)
                                .build();
                        postImageRepository.save(postImage);
                    }
                }
            }
        }
        // 💡 [수정] 4. 텍스트 내용 및 카테고리 수정
        // 🚨 누군가 글을 수정하면서 몰래 '공지' 카테고리로 바꾸려 한다면 관리자인지 검증!
        if ("공지".equals(request.category()) && !post.getUserEntity().isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED); // 권한 없음 에러
        }

        post.update(request.title(), request.content(), request.category());
    }

    /**
     * 게시글 ID 통한 삭제
     * 작성자와 삭제 요청자 userId 검증
     * 게시글을 DB에서 삭제전 imageUrl이 있다면 S3에서도 지우도록 코드 연결
     */
    public void deletePost(Long id, Long userId) {
        // 1. 삭제할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        // 2. 삭제 요청 보낸 유저 정보 조회
        UserEntity requestUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 3. 작성자와 삭제 요청자 일치 검증
        if (!post.getUserEntity().getId().equals(userId) && !requestUser.isAdmin()) {
            throw new CustomException(EDIT_ACCESS_DENIED);
        }
        // 4. 삭제 처리
        post.softDelete();
    }

    /**
     * 게시글 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponse> getAllPost(Pageable pageable, String keyword) {

        Page<PostEntity> posts = postRepository.findPostsWithNoticeOnTop(keyword, pageable);
        return posts.map(PostListResponse::from);
    }

    // ==========================================
    // 💡 [추가] 30일 지난 휴지통 게시글 및 S3 좀비 파일 일괄 삭제
    // ==========================================
    public void hardDeleteOldPosts() {
        // ✅ [TO-BE] application.yaml에서 가져온 변수 사용!
        LocalDateTime threshold = LocalDateTime.now().minusDays(postRetentionDays);

        // 1. 폭파시킬 S3 이미지 URL 목록 미리 가져오기
        List<String> imageUrlsToDelete = postRepository.findImageUrlsByOldDeletedPosts(threshold);

        // 2. DB에서 이미지 데이터 일괄 삭제
        postRepository.deleteImagesByOldDeletedPosts(threshold);

        // 3. DB에서 게시글 데이터 일괄 삭제
        int deletedPostCount = postRepository.deleteOldDeletedPosts(threshold);

        // 4. S3 클라우드에서 실제 물리 파일들 펑! 펑! 삭제
        if (!imageUrlsToDelete.isEmpty()) {
            for (String url : imageUrlsToDelete) {
                s3Service.deleteFile(url);
            }
        }

        if (deletedPostCount > 0) {
            log.info("🧹 [Data GC] 30일 경과 휴지통 게시글 {}개 및 S3 파일 {}장 영구 삭제 완료!", deletedPostCount, imageUrlsToDelete.size());
        }
    }
}
