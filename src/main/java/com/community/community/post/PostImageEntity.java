package com.community.community.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_images")
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // S3에 올라간 사진 주소
    @Column(nullable = false)
    private String imageUrl;

    /**
     * 내가 속한 게시글 (N:1 관계)
     * 여러장의 사진(Many)이 하나의 게시글(One)에 속함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @Builder
    public PostImageEntity(String imageUrl, PostEntity post) {
        this.imageUrl = imageUrl;
        this.post = post;
    }
}
