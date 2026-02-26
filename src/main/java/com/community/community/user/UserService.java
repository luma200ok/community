package com.community.community.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * 회원가입 기능
     */
    public Long singUp(UserEntity userEntity) {
        // 1. 이메일(username) 중복 검사
        validateDuplicateUsername(userEntity.getUsername());
        // 2. 이메일(email) 중복 검사
        validateDuplicateEmail(userEntity.getEmail());
        // 3. 데이터베이스에 저장
        userRepository.save(userEntity);
        // 4. 저장된 회원의 PK(id) 반환
        return userEntity.getId();
    }

    /**
     * 로그인 기능
     * @return 로그인 성공 시 회원 ID(PK) 반환
     */
    @Transactional(readOnly = true)
    public Long login(String username, String password) {
        // 1. 아이디로 회원 조회
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        // 2. 비밀번호 일치 여부 확인 (현시점 평문 비교, 차후 암호화 비교 업그레이드 예정)
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 아이디와 비밀번호 모두 일치 하여야 해당 회원의 ID 반환
        return user.getId();
    }

    private void validateDuplicateUsername(String username) {
        userRepository.findByUsername(username).ifPresent(m -> {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        });
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email).ifPresent(m -> {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        });
    }
}
