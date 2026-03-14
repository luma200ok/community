package com.community.community.user;

import com.community.community.security.jwt.JwtUtil;
import com.community.community.redis.RedisService;
import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.community.community.user.UserDto.PasswordFindRequest;
import static com.community.community.user.UserDto.TokenReissueRequest;
import static com.community.community.user.UserDto.TokenResponse;
import static com.community.community.user.UserDto.UserLoginRequest;
import static com.community.community.user.UserDto.UserSignupRequest;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    // promoteToAdmin yaml 환경변수
    @Value("${admin.secret-key}")
    private String adminSecretKey;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final JwtUtil jwtUtil;

    private final RedisService redisService;

    //회원가입 기능
    public void signUp(UserSignupRequest request) {
        // 1. 이름,이메일 중복 검사
        validateDuplicateUsername(request.username());
        validateDuplicateEmail(request.email());

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 엔티티 조립
        UserEntity userEntity = UserEntity.builder()
                .username(request.username())
                .password(encodedPassword)
                .email(request.email())
                .hintAnswer(request.hintAnswer())
                .build();

        // 4. DB 저장
        userRepository.save(userEntity);
    }

    /**
     * 로그인 기능
     * @return 로그인 성공 시 회원 ID(PK) 반환
     * 반환 타입을 로그인 성공 시 TokenResponse (Access, Refresh) 반환
     */
    @Transactional(readOnly = true)
    public TokenResponse login(UserLoginRequest request) {
        // 1. 아이디로 회원 조회
        UserEntity user = getValidUser(request.username());

        // 2. 비밀번호 일치 여부 확인 (현시점 평문 비교, 차후 암호화 비교 업그레이드 예정)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. Access Token & Refresh Token 발급
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        // 4. Refresh Token을 레디스에 저장
        redisService.setValues(
                "RT:" + user.getId(),
                refreshToken,
                Duration.ofDays(7));

        // 5. 두 토큰을 DTO에 담아 반환
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급 (Reissue)
     */
    @Transactional(readOnly = true)
    public TokenResponse reissue(TokenReissueRequest request) {
        String refreshToken = request.refreshToken();

        // 1. 넘어온 Refresh Token 제차게 정상적인지(위조/만료) 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 토큰을 열어서 안에 있는 유저 번호(userId) 꺼내기
        Long userId = jwtUtil.getClaims(refreshToken).get("userId", Long.class);

        // 3. 레디스 금고에서 해당 유저 Refresh Token 꺼내기
        String savedRefreshToken = redisService.getValues("RT:" + userId);

        // 4. 금고에 없거나, 유저가 보낸 토큰과 다르면 에러 (탈취 방지)
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 새 Access Token을 만들려면 username이 필요하므로 DB에서 유저 조회
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 6. 검증 완료! 새로운 토큰 세트 발급
        String newAccessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.createRefreshToken(user.getId());

        // 7. 레디스 금고 업데이트 (새 토큰으로 교체, 수명7일 연장)
        redisService.setValues(
                "RT:" + user.getId(),
                newRefreshToken,
                Duration.ofDays(7));

        // 8. 새 토큰 반환
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId) {
        redisService.deleteValues("RT:" + userId);
    }

    /**
     * 숨겨진 관리자 승급 로직
     */
    @Transactional
    public void promoteToAdmin(String username, String secretKey) {

        // 2. 비밀키가 틀리면 바로 쫓아냄 (권한 에러 던지기)
        if (!adminSecretKey.equals(secretKey)) {
            throw new CustomException(ErrorCode.EDIT_ACCESS_DENIED);
        }

        // 3. 비밀키가 맞으면 유저를 찾아 승급!
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.promoteToAdmin();
    }

    /**
     * 비밀번호 찾기 (임시 비밀번호 발급 및 이메일 전송)
     */
    @Transactional
    public void resetPasswordAndSendEmail(PasswordFindRequest request) {
        // 1. 아이디와 이메일로 회원 조회
        UserEntity user = userRepository.findByUsernameAndEmail(request.username(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 💡 방어막 1: 힌트 정답이 일치하는지 확인! (틀리면 에러 뱉고 컷!)
        if (!user.getHintAnswer().equals(request.hintAnswer())) {
            throw new IllegalArgumentException("힌트 정답이 일치하지 않습니다.");
        }

        // 💡 방어막 2: 5분 쿨타임이 지났는지 확인! (안 지났으면 컷!)
        if (!user.canSendEmail()) {
            throw new IllegalArgumentException("메일 발송은 5분에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.");
        }

        // --- 여기서부터는 기존 로직과 동일 ---
        String tempPassword = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user.updatePassword(passwordEncoder.encode(tempPassword));

        // 💡 쿨타임 타이머 리셋! (현재 시간으로 업데이트)
        user.updateEmailSentAt();

        String emailTitle = "🔥 Toy Community 임시 비밀번호 발급 안내";
        String emailContent = "임시 비밀번호 : [ " + tempPassword + " ]\n\n반드시 마이페이지에서 비밀번호를 변경해 주세요!";
        mailService.sendEmail(user.getEmail(), emailTitle, emailContent);
    }

    private void validateDuplicateUsername(String username) {
        userRepository.findByUsername(username).ifPresent(
                m -> {throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        });
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findActiveUserByEmail(email).ifPresent(
                m -> {throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        });
    }

    private UserEntity getValidUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
