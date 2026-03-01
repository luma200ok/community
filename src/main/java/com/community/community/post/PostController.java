package com.community.community.post;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.post.PostDto.PostCreateRequest;
import static com.community.community.post.PostDto.PostDetailResponse;
import static com.community.community.post.PostDto.PostListResponse;
import static com.community.community.post.PostDto.PostUpdateRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<String> writePost(
            @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        Long postId = postService.writePost(request, userId);

        return ResponseEntity.ok("게시글 작성이 완료되었습니다. 글 번호:" + postId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {

        // URL에서 뽑아낸 ID값을 서비스에 넘겨 DTO 받음
        PostDetailResponse response = postService.getPost(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updatePost(
            @PathVariable Long id,
            @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Long userId) {

        postService.updatePost(id, request, userId);

        return ResponseEntity.ok("게시글 수정이 완료되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        postService.deletePost(id,userId);

        return ResponseEntity.ok("게시글 삭제가 완료되었습니다.");
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> getAllPost(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt",direction = Sort.Direction.DESC)
            Pageable pageable) {
        // 클라이언트가 page,size 안보내면 기본값 설정

        // 1. 서비스에서 전체 글 목록을 가져옴
        Page<PostListResponse> responses = postService.getAllPost(pageable,keyword);

        return ResponseEntity.ok(responses);
    }
}
