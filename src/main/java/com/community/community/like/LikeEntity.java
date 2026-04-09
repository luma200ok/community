package com.community.community.like;

import com.community.community.common.BaseTimeEntity;
import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_like", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_like_user_post", columnNames = {"user_id", "post_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LikeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity postEntity;

    @Builder
    public LikeEntity(UserEntity user, PostEntity post) {
        this.userEntity = user;
        this.postEntity = post;
    }
}
