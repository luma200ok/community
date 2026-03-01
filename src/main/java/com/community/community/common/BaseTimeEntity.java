package com.community.community.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 자식 엔티티에 내 필드를 물려줌
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {

    @CreatedDate // 생성될 때 자동으로 시간 캡처
    @Column(updatable = false) // 값 생성 시 수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정될 때마다 자동으로 시간 갱신
    private LocalDateTime updatedAt;
}
