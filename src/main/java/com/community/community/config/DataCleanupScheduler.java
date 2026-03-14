package com.community.community.config;

import com.community.community.comment.CommentService;
import com.community.community.common.S3Service;
import com.community.community.post.PostImageRepository;
import com.community.community.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DataCleanupScheduler {

    private final CommentService commentService;
    private final PostService postService;

    private final PostImageRepository postImageRepository;
    private final S3Service s3Service;

    // 매일 새벽 3시에 실행 (cron 표현식: 초 분 시 일 월 요일)
    @Scheduled(cron = "${app.scheduler.cleanup-cron}")
    public void executeGarbageCollection() {
        log.info("=======================================");
        log.info("🧹 [Data GC] 새벽 청소 배치를 시작합니다...");

        // 1단계: 뼈대만 남은 '고아 댓글' 영구 삭제
        commentService.hardDeleteOrphanComments();

        // 2단계: 30일 지난 '휴지통 게시글' + 그 글에 달렸던 'S3 파일' 영구 삭제
        postService.hardDeleteOldPosts();

        // 3단계: 🧟 [찐 좀비 사냥] DB 주소록에 없는 S3 고아 파일(작성 취소된 사진 등) 폭파!
        List<String> validImageUrls = postImageRepository.findAllImageUrls(); // 정상 주소록 싹 가져오기
        s3Service.cleanUpZombieFiles(validImageUrls); // 비교해서 S3에서 지우기

        log.info("✨ [Data GC] 새벽 청소 배치가 완료되었습니다.");
        log.info("=======================================");
    }
}
