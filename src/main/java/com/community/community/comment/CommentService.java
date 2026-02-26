package com.community.community.comment;

import com.community.community.post.PostEntity;
import com.community.community.post.PostRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
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

    public Long writeComment(Long postId, CommentCreateRequest request) {

        // 1. 댓글을 다는 사용자(User)가 존재 하는지 확인
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 댓글이 달릴 게시글(Post)가 존재 하는지 확인
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

//    @Transactional(readOnly = true)
//    public

    public Long updateComment(Long postId,Long commentId, CommentUpdateRequest request) {
        // 1. 수정할 댓글 DB에서 조회
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 2. 주소 postId와 실제 postId가 같은지 검증
        if (!comment.getPostEntity().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }

        // 3. 내용 변경 (더티 체킹)
        comment.update(request.content());

        return comment.getId();
    }

    public void deleteComment(Long commentId) {
        // 1. 삭제할 댓글 DB에서 조회
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 2. DB에서 삭제
        commentRepository.delete(comment);
    }
}
