package com.community.community.post;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository  extends JpaRepository<PostEntity, Long> {
}
