package com.community.community.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.community.community.post.PostDto.PostCreateRequest;
import static com.community.community.post.PostDto.PostDetailResponse;
import static com.community.community.post.PostDto.PostListResponse;
import static com.community.community.post.PostDto.PostUpdateRequest;

@Tag(name = "📝 게시글 API", description = "게시글 작성, 조회, 수정, 삭제를 담당하는 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "새 게시글 작성(이미지 포함)",
            description = "제목과 내용을 입력받고, 선택적으로 이미지를 첨부하여 새로운 게시글을 등록합니다.\n\n" +
                    "**요청 데이터:**\n" +
                    "- `request`: 게시글 제목(`title`)과 내용(`content`) (application/json)\n" +
                    "- `image`: 첨부할 이미지 파일 (선택 사항)\n" +
                    "**권한:** **JWT 토큰이 필수**이며, 로그인 인증에 성공한 사용자만 작성할 수 있습니다.")
    // 1. 사진(파일)을 받기 위해 consumes 속성을 MULTIPART_FORM_DATA로 변경!
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> writePost(
            // 2. @RequestBody 대신 @RequestPart를 사용하여 JSON 데이터와 파일을 따로 받습니다.
            @Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
            @RequestPart(value = "request") PostCreateRequest request,
            // 3. 이미지는 없을 수도 있으니 required = false
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        Long postId = postService.writePost(request, image, userId);

        return ResponseEntity.ok("게시글 작성이 완료되었습니다. 글 번호:" + postId);
    }

    @Operation(summary = "게시글 단건 조회",
            description = "게시글 ID를 통해 특정 게시글의 상세 정보와 댓글 목록을 조회합니다.\n\n" +
                    "**요청 데이터:** 조회할 게시글 번호(`id`)\n")
    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {

        // URL에서 뽑아낸 ID값을 서비스에 넘겨 DTO 받음
        PostDetailResponse response = postService.getPost(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 수정",
            description = "특정 게시글의 제목과 내용을 수정합니다.\n\n" +
                    "**요청 데이터:** 수정할 제목(`title`)과 내용(`content`)\n" +
                    "**권한:** JWT 토큰이 필요하며, **게시글 작성자 본인만** 수정할 수 있습니다." +
                    " (작성자가 아닐 경우 `403 Forbidden` 반환)")
    @PutMapping("/{id}")
    public ResponseEntity<String> updatePost(
            @PathVariable Long id,
            @RequestBody PostUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        postService.updatePost(id, request, userId);

        return ResponseEntity.ok("게시글 수정이 완료되었습니다.");
    }

    @Operation(summary = "게시글 삭제",
            description = "특정 게시글의 삭제합니다.\n\n" +
                    "**요청 데이터:** 삭제할 게시글 번호(`id`)\n" +
                    "**권한:** JWT 토큰이 필요하며, **게시글 작성자 본인만** 삭제할 수 있습니다." +
                    " (작성자가 아닐 경우 `403 Forbidden` 반환)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        postService.deletePost(id,userId);

        return ResponseEntity.ok("게시글 삭제가 완료되었습니다.");
    }

    @Operation(summary = "게시글 목록 조회",
            description = "전체 게시글 목록을 페이징하여 조회하며, 키워드 검색 기능을 지원합니다.\n\n" +
                    "**요청 데이터:**\n" +
                    "- `keyword`: 제목 또는 내용 검색어 (선택)\n" +
                    "- `page`, `size`, `sort`: 페이징 및 정렬 파라미터 (기본값: 최신순 10개)")
    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> getAllPost(
            @Parameter(description = "검색 키워드 (제목 또는 내용에 포함된 단어)")
            @RequestParam(required = false) String keyword,
            @Parameter(
                    description = "페이지 정보 (예시 데이터를 참고하세요)",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": \"createdAt,desc\"}"
            )
            @PageableDefault(size = 10, sort = "createdAt",direction = Sort.Direction.DESC)
            Pageable pageable) {
        // 클라이언트가 page,size 안보내면 기본값 설정
        // 1. 서비스에서 전체 글 목록을 가져옴
        Page<PostListResponse> responses = postService.getAllPost(pageable,keyword);

        return ResponseEntity.ok(responses);
    }
}
