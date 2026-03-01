package com.community.community.comment;

import com.community.community.config.JwtUtil;
import com.community.community.post.PostEntity;
import com.community.community.post.PostRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.community.community.comment.CommentDto.*;

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 댓글이 게시될 게시글(Post)이 존재 하는지 확인
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 2. 주소 postId와 실제 postId 일치 검증
        if (!comment.getPostEntity().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }

        // 3. 댓글 작성자와 수정 요청자의 ID 일치 검증
        if (!comment.getUserEntity().getId().equals(userId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }
        // 4. 내용 변경 (더티 체킹)
        comment.update(request.content());
    }

    public void deleteComment(Long postId, Long commentId, Long userId) {
        // 1. 삭제할 댓글 DB에서 조회
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 2. 주소 postId와 실제 postId 일치 검증
        if (!comment.getPostEntity().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }

        // 3. 댓글 작성자와 삭제 요청자의 ID 일치 검증
        if (!comment.getUserEntity().getId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        // 4. DB에서 삭제
        commentRepository.delete(comment);
    }
}
