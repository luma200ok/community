package com.community.community.comment;

import com.community.community.common.BaseTimeEntity;
import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment")
public class CommentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity postEntity;

    // 부모 댓글이 지워지면 대댓글도 싹 지워지게 CASCADE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommentEntity parent;

    // 이 댓글에 달린 대댓글 리스트
    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    private List<CommentEntity> children = new ArrayList<>();

    @Builder
    public CommentEntity(String content, UserEntity userEntity, PostEntity postEntity) {
        this.content = content;
        this.userEntity = userEntity;
        this.postEntity = postEntity;
    }

    public void update(String content) {
        this.content = content;
    }

    // 부모-자식 관계를 맺어주는 편의 메서드
    public void addReply(CommentEntity reply) {
        this.children.add(reply);
        reply.setParent(this);
    }

    // soft delete 편의 메서드
    public void softDelete() {
        this.isDeleted = true;
    }


    // private setter (내부에서 부모를 세팅할 때 사용)
    private void setParent(CommentEntity parent) {
        this.parent = parent;
    }

}
