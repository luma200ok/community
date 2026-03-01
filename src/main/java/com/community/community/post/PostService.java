package com.community.community.post;

import com.community.community.comment.CommentEntity;
import com.community.community.comment.CommentRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.community.community.post.PostDto.*;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public Long writePost(PostCreateRequest request, Long userId) {
        // 1. 글을 작성하는 사용자를 DB에서 조회
        UserEntity findUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 앟는 회원입니다."));

        // 2. DTO의 User를 PostEntity 조합
        PostEntity post = PostEntity.builder()
                .title(request.title())
                .content(request.content())
                .userEntity(findUser) // 유저 객체 맵핑
                .build();

        // 3. DB에 저장
        postRepository.save(post);

        return post.getId();
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long id) {
        // 1. 글 번호를 통해 DB에서 게시글 조회. 없으면 에러
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 게시글에 달린 댓글 가져오기
        List<CommentEntity> comments = commentRepository.findByPostEntityId(id);

        // 2. 찾은 Entity를 DTO로 변환해서 반환
        return PostDetailResponse.from(post, comments);
    }

    public void updatePost(Long id, PostUpdateRequest request, Long userId) {
        // 1. 수정할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 작성자와 수정 요청자 일치 검증
        if (!post.getUserEntity().getId().equals(userId)) {
            throw new AccessDeniedException("작성자만 수정할 수 있습니다.");
        }

        // 3. 통과시 수정
        post.update(request.title(), request.content());
    }

    public void deletePost(Long id,Long userId) {
        // 1. 삭제할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 작성자와 삭제 요청자 일치 검증
        if (!post.getUserEntity().getId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        // 3. 찾은 게시글을 DB에서 삭제.
        postRepository.delete(post);
    }

    /*
    @Transactional(readOnly = true)
    public List<PostListResponse> getAllPost() {
        // 1. 게시글 List 조회
        List<PostEntity> posts = postRepository.findAll();

        return posts.stream()
                .map(PostListResponse::from).toList();
    }
    */

    @Transactional(readOnly = true)
    public Page<PostListResponse> getAllPost(Pageable pageable) {
        // 1. 게시글 List 조회
        Page<PostEntity> posts = postRepository.findAll(pageable);

        return posts.map(PostListResponse::from);
    }
}
