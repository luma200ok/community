package com.community.community.comment;

import java.util.List;

public interface CommentRepositoryCustom {

    List<CommentEntity> findCommentsByPostIdWithUser(Long postId);
}
