package com.community.community.mypage;

import com.community.community.comment.CommentRepository;
import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import com.community.community.like.LikeEntity;
import com.community.community.like.LikeRepository;
import com.community.community.post.PostRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.community.community.comment.CommentDto.*;
import static com.community.community.mypage.MyPageDto.*;
import static com.community.community.post.PostDto.*;

@Service
@Transactional
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 1. 비밀번호 변경
     */
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedNewPassword);
    }

    /**
     * 2. 내가 쓴 글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponse> getMyPosts(Long userId, Pageable pageable) {
        return postRepository.findByUserEntity_id(userId, pageable)
                .map(PostListResponse::from);
    }

    /**
     * 3. 내가 쓴 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getMyComments(Long userId, Pageable pageable) {
        return commentRepository.findByUserEntity_id(userId, pageable)
                .map(CommentResponse::from);
    }

    /**
     * 4. 내가 좋아요 누른 글 조회
     */
    @Transactional(readOnly = true)
    public Page<PostListResponse> getMyLikePosts(Long userId, Pageable pageable) {
        // 1. 유저 ID로 LikeEntity 목록을 가져옴
        // 2. LikeEntity 안에 들어있는 PostEntity를 꺼냄
        // 3. PostEntity를 PostListResponse DTO로 변환
        return likeRepository.findByUserEntity_Id(userId, pageable)
                .map(LikeEntity::getPostEntity)
                .map(PostListResponse::from);
    }
}
