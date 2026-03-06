package com.community.community.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 1. 권한 필드 추가 (기본값 일반 유저)
    @Column(nullable = false)
    private String role = "USER";

    // 2. 관리자(Admin) 확인 편의 메서드 추가
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    @Builder
    public UserEntity(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Builder
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 관리자로 승급시키는 메서드
    public void promoteToAdmin() {
        this.role = "ADMIN";
    }
}
