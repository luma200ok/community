package com.community.community.comment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.comment.CommentDto.*;

@Tag(name = "📝 댓글 API", description = "댓글 작성, 조회, 수정, 삭제를 담당하는 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 작성",
            description = "특정 게시글에 새로운 댓글을 등록합니다.\n\n" +
                    "**요청 데이터:** 댓글 내용(`content`)\n" +
                    "**권한:** **JWT 토큰이 필수**이며, 로그인한 사용자만 댓글을 작성할 수 있습니다.")
    @PostMapping
    public ResponseEntity<String> writeComment(
            @PathVariable Long postId,
            @RequestBody CommentCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        Long commentId = commentService.writeComment(postId, request,userId);

        return ResponseEntity.ok("댓글 작성이 완료되었습니다. 댓글 번호: " + commentId);
    }

    @Operation(
            summary = "댓글 수정",
            description = "작성한 댓글의 내용을 수정합니다.\n\n" +
                    "**요청 데이터:** 수정할 댓글 내용(`content`)\n" +
                    "**권한:** JWT 토큰이 필요하며, **댓글 작성자 본인만** 수정할 수 있습니다." +
                    " (작성자가 아닐 경우 `403 Forbidden` 반환)")
    @PutMapping("/{commentId}")
    public ResponseEntity<String> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        commentService.updateComment(postId,commentId, request, userId);
        return ResponseEntity.ok("댓글 수정이 완료되었습니다.");
    }

    @Operation(
            summary = "댓글 삭제",
            description = "작성한 댓글을 삭제합니다.\n\n" +
                    "**권한:** JWT 토큰이 필요하며, **댓글 작성자 본인만** 삭제할 수 있습니다." +
                    " (작성자가 아닐 경우 `403 Forbidden` 반환)")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
            ) {
        commentService.deleteComment(postId,commentId,userId);
        return ResponseEntity.ok("댓글 삭제가 완료되었습니다.");
    }
}
