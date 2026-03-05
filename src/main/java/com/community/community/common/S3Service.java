package com.community.community.common;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    // 💡 방금 설치한 라이브러리가 제공하는 S3 조종기입니다!
    private final S3Template s3Template;

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
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        // S3에 올리기 전에 무조건 확장자 검사부터
        validateImageFileExtension(file.getOriginalFilename());

        // 1. 파일 이름 중복을 막기 위해 겹치지 않는 랜덤 이름(UUID) 생성
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String randomFileName = UUID.randomUUID().toString() + extension;

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
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            s3Template.deleteObject(bucketName, fileName);
            System.out.println("S3 파일 삭제 완료: " + fileName);
        } catch (Exception e) {
            System.out.println("S3 파일 삭제 실패: " + e.getMessage());
        }
    }

    // 1. 파일 확장자를 검사하는 메서드
    private void validateImageFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        List<String> allowedExtensionList = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "pdf");

        if (!allowedExtensionList.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자 입니다. (허용:jpg, jpeg, png, gif, webp,pdf)");
        }
    }
}
