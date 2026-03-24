package com.community.community.comment;

import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import com.community.community.post.PostEntity;
import com.community.community.post.PostRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.community.community.comment.CommentDto.*;
import static com.community.community.exception.ErrorCode.COMMENT_MISMATCH;
import static com.community.community.exception.ErrorCode.COMMENT_NOT_FOUND;
import static com.community.community.exception.ErrorCode.POST_NOT_FOUND;
import static com.community.community.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public Long writeComment(Long postId, CommentCreateRequest request, Long userId) {

        // 1. 댓글을 작성하는 사용자(User)가 존재 하는지 확인
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // 2. 댓글이 게시될 게시글(Post)이 존재 하는지 확인
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        // 댓글 작성시 댓글 수 +1
        post.increaseCommentCount();

        // 3. 빌더를 이용해 모든 연관관계 묶기
        CommentEntity comment = CommentEntity.builder()
                .content(request.content())
                .userEntity(user)
                .postEntity(post)
                .build();

        commentRepository.save(comment);

        return comment.getId();
    }

    public void updateComment(
            Long postId, Long commentId, CommentUpdateRequest request, Long userId) {
        // 1. 수정할 댓글 DB에서 조회
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 2. 주소 postId와 실제 postId 일치 검증
        if (!comment.getPostEntity().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_MISMATCH);
        }

        // 3. 댓글 작성자와 수정 요청자의 ID 일치 검증
        if (!comment.getUserEntity().getId().equals(userId)) {
            throw new CustomException(ErrorCode.EDIT_ACCESS_DENIED);
        }
        // 4. 내용 변경 (더티 체킹)
        comment.update(request.content());
    }

    public void deleteComment(Long postId, Long commentId, Long userId) {
        // 1. 삭제할 댓글 DB에서 조회
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 2. 주소 postId와 실제 postId 일치 검증
        if (!comment.getPostEntity().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_MISMATCH);
        }

        // 3. 삭제 요청 유저 정보 조회, 작성자 본인 이거나 관리자 인지 검증 둘다 아니면 에러
        UserEntity requestUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!comment.getUserEntity().getId().equals(userId) && !requestUser.isAdmin()) {
            throw new CustomException(ErrorCode.EDIT_ACCESS_DENIED);
        }

        comment.getPostEntity().decreaseCommentCount();

        // 4. 똑똑한 삭제 로직
        // 내 밑에 대댓글(자식)이 존재한다면 DB에서 지우지 않고 '삭제된 상태'로만 변경!
        if (!comment.getChildren().isEmpty()) {
            comment.softDelete();
        } else {
            // 대댓글이 하나도 없다면 미련 없이 DB에서 진짜 삭제!
            commentRepository.delete(comment);
        }
    }

    public Long writeReply(Long postId, Long parentId, CommentCreateRequest request, Long userId) {

        // 1. 유저와 게시글 검증
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(POST_NOT_FOUND));

        // 2. 부모 댓글 존재 여부 검증
        CommentEntity parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(COMMENT_NOT_FOUND));

        // 3. 부모 댓글이 현재 게시글의 댓글이 맞는지 검증 (엉뚱한 글의 댓글에 답글 방지)
        if (!parentComment.getPostEntity().getId().equals(postId)) {
            throw new CustomException(COMMENT_MISMATCH);
        }

        // 4. 대댓글 생성 (게시글의 전체 댓글 수 +1)
        post.increaseCommentCount();

        CommentEntity reply = CommentEntity.builder()
                .content(request.content())
                .userEntity(user)
                .postEntity(post)
                .build();

        parentComment.addReply(reply);

        commentRepository.save(reply);

        return reply.getId();
    }

    // ==========================================
    // [추가] 고아 댓글 영구 삭제 (스케줄러에서 호출할 메서드)
    // ==========================================
    public void hardDeleteOrphanComments() {
        int deletedCount;
        int totalDeleted = 0;

        // 💡 Bottom-Up 방식: 맨 밑의 자식부터 부모까지 더 이상 지워질 게 없을 때까지 깎아 올라감
        do {
            deletedCount = commentRepository.deleteOrphanComments();
            totalDeleted += deletedCount;
        } while (deletedCount > 0);

        if (totalDeleted > 0) {
            log.info("🧹 [Data GC] 뼈대만 남은 고아 댓글 총 {}개 영구 삭제 완료!", totalDeleted);
        }
    }
}
