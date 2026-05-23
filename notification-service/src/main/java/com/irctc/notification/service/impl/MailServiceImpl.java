package com.irctc.notification.service.impl;

import com.irctc.notification.dto.NotificationRequest;
import com.irctc.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;

    @Override
    public void sendEmail(NotificationRequest request) {
        log.info("Processing template-based email for {}", request.getToEmail());

        String templateFile;
        String subject;

        switch (request.getTemplateType().toUpperCase()) {
            case "EMAIL_VERIFICATION":
                templateFile = "classpath:templates/email-verification.txt";
                subject = "IRCTC Clone - Email Verification OTP";
                break;
            case "PASSWORD_RESET":
                templateFile = "classpath:templates/password-reset.txt";
                subject = "IRCTC Clone - Password Reset OTP";
                break;
            default:
                log.error("Unknown template type: {}", request.getTemplateType());
                return;
        }

        try {
            // 1. Read the template file from resources
            Resource resource = resourceLoader.getResource(templateFile);
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            String rawTemplate = FileCopyUtils.copyToString(reader);

            // 2. Resolve template placeholders using templateModel variables
            String resolvedBody = rawTemplate;
            if (request.getTemplateModel() != null) {
                for (Map.Entry<String, String> entry : request.getTemplateModel().entrySet()) {
                    resolvedBody = resolvedBody.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }

            // 3. Send email (with fallback output)
            triggerSend(request.getToEmail(), subject, resolvedBody);

        } catch (Exception ex) {
            log.error("Failed to load or resolve template: {}", ex.getMessage());
        }
    }

    private void triggerSend(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception ex) {
            log.warn("==================================================================");
            log.warn("SMTP EMAIL SENDING FAILED FOR: {}", to);
            log.warn("REASON: {}", ex.getMessage());
            log.warn("FALLBACK MODE: Printing dynamically resolved message below.");
            log.warn(">>> RECIPIENT: {}", to);
            log.warn(">>> SUBJECT: {}", subject);
            log.warn(">>> BODY: \n{}", body);
            log.warn("==================================================================");
        }
    }
}
