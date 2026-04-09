package com.community.community.common;

import com.community.community.exception.CustomException;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.community.community.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    // 라이브러리가 제공하는 S3 조종기
    private final S3Template s3Template;
    // S3 뒤지기 위한 오리지널 클라이언트
    private final S3Client s3Client;

    // 폴더 경로를 읽어올 변수 추가
    @Value("${spring.cloud.aws.s3.folder}")
    private String s3Folder;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 이미지를 S3에 업로드하고 접속 URL을 반환하는 메서드
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new CustomException(FILE_NOT_FOUND);
        }

        // S3에 올리기 전에 확장자 + 실제 MIME 타입 검사
        validateImageFileExtension(file.getOriginalFilename());
        validateMimeType(file);

        // 1. 파일 이름 중복을 막기 위해 겹치지 않는 랜덤 이름(UUID) 생성
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String randomFileName = s3Folder + "/" + UUID.randomUUID().toString() + extension;

        try {
            // 2. S3 조종기를 통해 버킷에 파일 업로드!
            s3Template.upload(bucketName, randomFileName, file.getInputStream(), null);

            // 3. 업로드된 이미지를 볼 수 있는 S3 퍼블릭 URL을 만들어서 반환
            // 형식: https://[버킷이름].s3.[리전].amazonaws.com/[파일이름]
            return "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + randomFileName;

        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 업로드한 이미지 삭제
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            // ❌ [AS-IS] 폴더명(local/ 또는 prod/)이 짤려나가서 S3가 파일을 못 찾음
            // String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            // ✅ [TO-BE] ".com/" 이후의 문자열을 통째로 추출하여 폴더 경로까지 포함!
            // 예: https://버킷명.s3.리전.amazonaws.com/local/파일.png -> "local/파일.png" 추출
            String fileKey = fileUrl.substring(fileUrl.indexOf(".com/") + 5);

            s3Template.deleteObject(bucketName, fileKey);
            log.info("S3 파일 삭제 완료: {}", fileKey); // System.out 대신 log 사용 추천!
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
        }
    }

    // 1. 파일 확장자를 검사하는 메서드
    private void validateImageFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new CustomException(FILE_EXTENSION_NOT_FOUND);
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        List<String> allowedExtensionList = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "pdf");

        if (!allowedExtensionList.contains(extension)) {
            throw new CustomException(INVALID_FILE_EXTENSION);
        }
    }

    // 2. 실제 파일 바이너리(Magic Byte)를 읽어 MIME 타입을 검증하는 메서드
    // 확장자만 바꿔서 위장한 악성 파일을 차단
    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png",  "png",
            "image/gif",  "gif",
            "image/webp", "webp",
            "application/pdf", "pdf"
    );

    private void validateMimeType(MultipartFile file) {
        Tika tika = new Tika();
        try (InputStream inputStream = file.getInputStream()) {
            String detectedMime = tika.detect(inputStream);
            if (!ALLOWED_MIME_TYPES.containsKey(detectedMime)) {
                throw new CustomException(INVALID_FILE_EXTENSION);
            }
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("파일 MIME 타입 검증에 실패했습니다.", e);
        }
    }

    public void cleanUpZombieFiles(List<String> dbValidUrls) {
        log.info("🕵️‍♂️ [S3 Zombie Hunter] 좀비 파일 탐색을 시작합니다...");

        int zombieCount = 0;

        try {
            // 1. S3 버킷(우리가 지정한 폴더)에 있는 모든 파일 목록 긁어오기 요청서 작성
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Folder + "/") // 예: local/ 또는 prod/ 폴더 안만 검사
                    .build();

            // 2. S3에 요청서 제출하고 목록 받아오기
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            // 3. S3에 있는 파일들을 하나씩 꺼내서 DB 주소록과 비교!
            for (S3Object s3Object : listResponse.contents()) {
                String s3FileKey = s3Object.key(); // (예: local/uuid.png)

                // 해당 파일의 완전한 S3 접속 URL을 조립 (DB에 저장된 형태와 똑같이 만들기 위함)
                String s3FileUrl = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + s3FileKey;

                // 4. 만약 DB 주소록(dbValidUrls)에 이 URL이 없다면? -> 🧟 무단 침입 좀비 당첨!
                if (!dbValidUrls.contains(s3FileUrl)) {
                    s3Template.deleteObject(bucketName, s3FileKey);
                    zombieCount++;
                    log.info("🔫 [S3 Zombie Hunter] 좀비 처형 완료: {}", s3FileKey);
                }
            }

            log.info("✨ [S3 Zombie Hunter] 탐색 종료. 총 {}개의 좀비 파일을 제거했습니다.", zombieCount);

        } catch (Exception e) {
            log.error("❌ 좀비 파일 청소 중 오류 발생: {}", e.getMessage());
        }
    }
}
