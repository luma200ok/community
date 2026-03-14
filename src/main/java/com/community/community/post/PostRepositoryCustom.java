package com.community.community.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<PostEntity> findPostsWithNoticeOnTop(String keyword, Pageable pageable);
}
