package com.community.community.like;

import com.community.community.post.PostEntity;
import com.community.community.post.PostRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * 좋아요 토글 로직
     *
     * @return true : 좋아요 등록, false: 좋아요 취소
     */

    public boolean toggleLike(Long postId, Long userId) {
        // 1. 유저와 게시글 존재 확인
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        PostEntity post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 이미 이 유저가 이 게시글에 좋아요 눌렀는지 DB 조회
        Optional<LikeEntity> foundLike = likeRepository.findByUserEntityAndPostEntity(user, post);


        if (foundLike.isPresent()) {
            // 3-1 이미 좋아요가 있다면? -> 삭제 (좋아요 취소)
            likeRepository.delete(foundLike.get());
            post.decreaseLikeCount();
            return false;
        } else {
            // 3-2 좋아요가 없다면? -> 생성 (좋아요 등록)
            LikeEntity build = LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build();
            likeRepository.save(build);
            post.increaseLikeCount();
            return true;
        }

    }
}
