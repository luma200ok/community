package com.community.community.mypage;

import com.community.community.comment.CommentDto;
import com.community.community.comment.CommentEntity;
import com.community.community.post.PostDto;
import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class MyPageDto {

    public record PasswordUpdateRequest(
            String currentPassword,
            String newPassword
    ) {
    }

    // 내 정보 조회 응답
    public record MyPageInfoResponse(
            String username,
            String email,
            String role,
            String hintAnswer
    ) {
        public static MyPageInfoResponse from(UserEntity user) {
            return new MyPageInfoResponse(
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getHintAnswer()
            );
        }
    }

    // 💡 힌트 수정 요청 추가
    public record HintUpdateRequest(
            String currentPassword,
            String newHintAnswer
    ) {}
}
