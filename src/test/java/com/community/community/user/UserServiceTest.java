package com.community.community.user;

import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import com.community.community.redis.RedisService;
import com.community.community.security.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.community.community.user.UserDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailService mailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisService redisService;

    // ───── 회원가입 ─────────────────────────────────
    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("정상 가입")
        void success() {
            given(userRepository.findByUsername("tester")).willReturn(Optional.empty());
            given(userRepository.findActiveUserByEmail("test@test.com")).willReturn(Optional.empty());
            given(passwordEncoder.encode(any())).willReturn("encoded");

            assertThatCode(() -> userService.signUp(
                new UserSignupRequest("tester", "pass1234", "test@test.com", "hint")
            )).doesNotThrowAnyException();

            then(userRepository).should().save(any(UserEntity.class));
        }

        @Test
        @DisplayName("중복 아이디 → DUPLICATE_USERNAME")
        void duplicateUsername() {
            given(userRepository.findByUsername("tester")).willReturn(Optional.of(mockUser()));

            assertThatThrownBy(() -> userService.signUp(
                new UserSignupRequest("tester", "pass1234", "test@test.com", "hint")
            )).isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getErrorCode())
              .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        }

        @Test
        @DisplayName("중복 이메일 → DUPLICATE_EMAIL")
        void duplicateEmail() {
            given(userRepository.findByUsername("tester")).willReturn(Optional.empty());
            given(userRepository.findActiveUserByEmail("dup@test.com")).willReturn(Optional.of(mockUser()));

            assertThatThrownBy(() -> userService.signUp(
                new UserSignupRequest("tester", "pass1234", "dup@test.com", "hint")
            )).isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getErrorCode())
              .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    // ───── 로그인 ─────────────────────────────────
    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 → Access/Refresh Token 반환")
        void success() {
            UserEntity user = mockUser();
            given(userRepository.findByUsername("tester")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("pass1234", "encodedPw")).willReturn(true);
            given(jwtUtil.createAccessToken(any(), anyString())).willReturn("access-token");
            given(jwtUtil.createRefreshToken(any())).willReturn("refresh-token");

            TokenResponse result = userService.login(new UserLoginRequest("tester", "pass1234"));

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            then(redisService).should().setValues(anyString(), eq("refresh-token"), any());
        }

        @Test
        @DisplayName("비밀번호 불일치 → INVALID_PASSWORD")
        void wrongPassword() {
            given(userRepository.findByUsername("tester")).willReturn(Optional.of(mockUser()));
            given(passwordEncoder.matches("wrong", "encodedPw")).willReturn(false);

            assertThatThrownBy(() -> userService.login(new UserLoginRequest("tester", "wrong")))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
        }

        @Test
        @DisplayName("존재하지 않는 유저 → USER_NOT_FOUND")
        void userNotFound() {
            given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(new UserLoginRequest("ghost", "pass")))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ───── 비밀번호 찾기 ─────────────────────────────────
    @Nested
    @DisplayName("비밀번호 찾기")
    class FindPassword {

        @Test
        @DisplayName("정상: 임시 비밀번호 발급 후 메일 전송")
        void success() {
            UserEntity user = mockUser(); // hintAnswer = "school"
            given(userRepository.findByUsernameAndEmail("tester", "test@test.com"))
                .willReturn(Optional.of(user));
            given(passwordEncoder.encode(any())).willReturn("newEncoded");
            willDoNothing().given(mailService).sendEmail(anyString(), anyString(), anyString());

            assertThatCode(() -> userService.resetPasswordAndSendEmail(
                new PasswordFindRequest("tester", "test@test.com", "school")
            )).doesNotThrowAnyException();

            then(mailService).should().sendEmail(eq("test@test.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("아이디+이메일 불일치 → USER_NOT_FOUND")
        void userNotFound() {
            given(userRepository.findByUsernameAndEmail("tester", "wrong@test.com"))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.resetPasswordAndSendEmail(
                new PasswordFindRequest("tester", "wrong@test.com", "school")
            )).isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getErrorCode())
              .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("힌트 정답 불일치 → INVALID_HINT_ANSWER")
        void wrongHint() {
            UserEntity user = mockUser(); // hintAnswer = "school"
            given(userRepository.findByUsernameAndEmail("tester", "test@test.com"))
                .willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.resetPasswordAndSendEmail(
                new PasswordFindRequest("tester", "test@test.com", "wrongHint")
            )).isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_HINT_ANSWER);
        }

        @Test
        @DisplayName("5분 쿨타임 미경과 → EMAIL_SEND_COOLDOWN")
        void emailCooldown() {
            UserEntity user = mockUser();
            // 방금 메일을 보낸 것처럼 lastEmailSentAt을 1분 전으로 세팅
            ReflectionTestUtils.setField(user, "lastEmailSentAt", LocalDateTime.now().minusMinutes(1));
            given(userRepository.findByUsernameAndEmail("tester", "test@test.com"))
                .willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.resetPasswordAndSendEmail(
                new PasswordFindRequest("tester", "test@test.com", "school")
            )).isInstanceOf(CustomException.class)
              .extracting(e -> ((CustomException) e).getErrorCode())
              .isEqualTo(ErrorCode.EMAIL_SEND_COOLDOWN);
        }

        @Test
        @DisplayName("5분 경과 후 재발급 가능")
        void afterCooldownSuccess() {
            UserEntity user = mockUser();
            ReflectionTestUtils.setField(user, "lastEmailSentAt", LocalDateTime.now().minusMinutes(6));
            given(userRepository.findByUsernameAndEmail("tester", "test@test.com"))
                .willReturn(Optional.of(user));
            given(passwordEncoder.encode(any())).willReturn("newEncoded");
            willDoNothing().given(mailService).sendEmail(anyString(), anyString(), anyString());

            assertThatCode(() -> userService.resetPasswordAndSendEmail(
                new PasswordFindRequest("tester", "test@test.com", "school")
            )).doesNotThrowAnyException();
        }
    }

    // ───── 로그아웃 ─────────────────────────────────
    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("정상 로그아웃 → Redis RT 삭제")
        void success() {
            userService.logout(1L);
            then(redisService).should().deleteValues("RT:1");
        }
    }

    // ───── 헬퍼 ─────────────────────────────────────
    private UserEntity mockUser() {
        return UserEntity.builder()
            .username("tester")
            .password("encodedPw")
            .email("test@test.com")
            .hintAnswer("school")
            .build();
    }
}
