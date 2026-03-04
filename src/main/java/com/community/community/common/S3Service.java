package com.community.community.common;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
            return null; // 첨부된 파일이 없으면 그냥 null 반환
        }

        // 1. 파일 이름 중복을 막기 위해 겹치지 않는 랜덤 이름(UUID) 생성
        String originalFilename = file.getOriginalFilename();
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

}
