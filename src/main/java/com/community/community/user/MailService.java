package com.community.community.user;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    // 라이브러리가 제공하는 메일발송 객체
    private final JavaMailSender javaMailSender;

    /**
     * 수신자 이메일, 제목, 내용을 받아 메일 발송 메서드
     */
    public void sendEmail(String toMail, String title, String text) {
        SimpleMailMessage emailForm = new SimpleMailMessage();
        emailForm.setTo(toMail);    // 수신자
        emailForm.setSubject(title); // 메일 제목
        emailForm.setText(text);    // 메일 내용

        javaMailSender.send(emailForm);
    }
}
