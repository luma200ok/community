package com.community.community.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImageEntity, Long> {

//    void deleteByPostId(Long postId); // PostEntity에 orphanRemoval = true가 켜져있어 필요없음.
}
