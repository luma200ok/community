package com.community.community.like;

import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import com.community.community.post.PostRepository;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * 좋아요 토글 로직
     * INSERT IGNORE로 check-then-act 완전 제거 — Race Condition 없음
     * - INSERT 성공(1) → 좋아요 등록, likeCount 원자적 증가
     * - INSERT 무시(0) → 이미 좋아요 상태 → 취소, likeCount 원자적 감소
     *
     * @return true: 좋아요 등록됨, false: 좋아요 취소됨
     */
    public boolean toggleLike(Long postId, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        int inserted = likeRepository.insertIgnore(userId, postId);
        if (inserted > 0) {
            postRepository.incrementLikeCount(postId);
            return true;
        } else {
            likeRepository.deleteByUserAndPost(userId, postId);
            postRepository.decrementLikeCount(postId);
            return false;
        }
    }
}
