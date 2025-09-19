
package org.example.seasontonebackend.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.mail.username", matchIfMissing = false)
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        if (emailSender == null || fromEmail == null || fromEmail.isEmpty()) {
            throw new IllegalStateException("메일 서비스가 설정되지 않았습니다. 환경변수를 확인해주세요.");
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
