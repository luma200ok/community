package com.community.community.mypage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.comment.CommentDto.CommentResponse;
import static com.community.community.post.PostDto.PostListResponse;

@Tag(name = "👤 마이페이지 API", description = "내 정보(비밀번호) 수정 및 내가 작성한 글/댓글, 좋아요 누른 글 목록을 조회합니다.")@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController{

    private final MyPageService myPageService;

    @Operation(summary = "비밀번호 변경",
            description = "현재 비밀번호와 새 비밀번호를 입력받아, 내 비밀번호를 안전하게 변경합니다.\n\n" +
                    "**권한:** **JWT 토큰이 필수**입니다.")
    @PatchMapping("/password")
    public ResponseEntity<String> updatePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody MyPageDto.PasswordUpdateRequest request
    ) {
        myPageService.updatePassword(userId, request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @Operation(summary = "내가 쓴 글 목록 조회")
    @GetMapping("/posts")
    public ResponseEntity<Page<PostListResponse>> getMyPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(
                    description = "페이지 정보",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": \"createdAt,desc\"}")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(myPageService.getMyPosts(userId, pageable));
    }

    @Operation(summary = "내가 쓴 댓글 목록 조회")
    @GetMapping("/comments")
    public ResponseEntity<Page<CommentResponse>> getMyComments(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(
                    description = "페이지 정보",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": \"createdAt,desc\"}")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(myPageService.getMyComments(userId, pageable));
    }

    @Operation(summary = "내가 누른 좋아요 조회")
    @GetMapping("/likes")
    public ResponseEntity<Page<PostListResponse>> getMyLikePosts(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "페이지 정보"
                    , example = "{\"page\": 0, \"size\": 10, \"sort\": \"createdAt,desc\"}")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(myPageService.getMyLikePosts(userId, pageable));
    }
}
