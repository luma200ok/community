package com.community.community.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    //  권한 필드 추가 (기본값 일반 유저)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 관리자(Admin) 확인 편의 메서드 추가
    public boolean isAdmin() {
        return Role.ADMIN == this.role;
    }

    // 비밀번호 찾기용 힌트 답변
    @Column
    private String hintAnswer;

    // 마지막 메일 발송 시간(쿨타임 체크용)
    @Column
    private LocalDateTime lastEmailSentAt;

    @Builder
    public UserEntity(String username, String password, String email, String hintAnswer) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.hintAnswer = hintAnswer;
        this.role = Role.USER;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 관리자로 승급시키는 메서드
    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    // 쿨타임(5분)이 지났는지 확인하는 편의 메서드
    public boolean canSendEmail() {
        if (this.lastEmailSentAt == null) return true; // 한 번도 보낸 적 없으면 통과
        return java.time.Duration.between(this.lastEmailSentAt, LocalDateTime.now()).toMinutes() >= 5;
    }

    // 메일 발송 시간 갱신 편의 메서드
    public void updateEmailSentAt() {
        this.lastEmailSentAt = LocalDateTime.now();
    }

    // 힌트 정답 변경 편의 메서드
    public void updateHintAnswer(String hintAnswer) {
        this.hintAnswer = hintAnswer;
    }
}
