package com.community.community.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface PostRepositoryCustom {

    Page<PostEntity> findPostsWithNoticeOnTop(String keyword, Pageable pageable);
    
    Optional<PostEntity> findByIdWithUserAndImages(Long id);
}
