package com.community.community.post;

import com.community.community.comment.CommentEntity;
import com.community.community.comment.CommentRepository;
import com.community.community.user.UserEntity;
import com.community.community.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.community.community.post.PostDto.*;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public Long writePost(PostCreateRequest request) {
        // 1. 글을 작성하는 사용자를 DB에서 조회
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 앟는 회원입니다."));

        // 2. DTO의 User를 조합하여 PostEntity 만듬
        PostEntity post = PostEntity.builder()
                .title(request.title())
                .content(request.content())
                .userEntity(user) // 게시글의 글쓴이와 연결
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

    public Long updatePost(Long id, PostUpdateRequest request) {
        // 1. 수정할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 찾아온 객체의 값을 수정
        post.update(request.title(), request.content());

        return post.getId();
    }

    public void deletePost(Long id) {
        // 1. 삭제할 게시글을 DB에서 조회
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 2. 찾은 게시글을 DB에서 삭제.
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostListResponse> getAllPost() {
        List<PostEntity> posts = postRepository.findAll();

        return posts.stream()
                .map(PostListResponse::from).toList();
    }
}
