package com.community.community.user;

import com.community.community.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.community.community.user.UserDto.*;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    //회원가입 기능
    public Long signUp(UserSignupRequest request) {
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
                .build();

        // 4. DB 저장
        userRepository.save(userEntity);
        // 5. 저장된 회원의 PK(id) 반환
        return userEntity.getId();
    }


    // 로그인 기능
    // @return 로그인 성공 시 회원 ID(PK) 반환
    // 반환 타입을 Long -> String 으로 변경 (토큰 사용)
    @Transactional(readOnly = true)
    public String login(UserLoginRequest request) {
        // 1. 아이디로 회원 조회
        UserEntity user = getValidUser(request.username());

        // 2. 비밀번호 일치 여부 확인 (현시점 평문 비교, 차후 암호화 비교 업그레이드 예정)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 아이디와 비밀번호 모두 일치 하여야 해당 회원의 ID 반환
        return jwtUtil.createToken(user.getId(), user.getUsername());
    }

    private void validateDuplicateUsername(String username) {
        userRepository.findByUsername(username).ifPresent(
                m -> {throw new IllegalStateException("이미 존재하는 아이디입니다.");
        });
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email).ifPresent(
                m -> {throw new IllegalStateException("이미 가입된 이메일입니다.");
        });
    }

    private UserEntity getValidUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
    }
}
