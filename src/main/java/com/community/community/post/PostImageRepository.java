package com.community.community.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImageEntity, Long> {

//    void deleteByPostId(Long postId); // PostEntity에 orphanRemoval = true가 켜져있어 필요없음.

    // ==========================================
    // 💡 [추가] DB에 살아있는 모든 정상 이미지 URL 주소록 조회
    // ==========================================
    @Query("SELECT p.imageUrl FROM PostImageEntity p")
    List<String> findAllImageUrls();
}
