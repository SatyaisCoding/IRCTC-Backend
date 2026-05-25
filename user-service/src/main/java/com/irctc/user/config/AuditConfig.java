package com.irctc.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AuditConfig {
    // Enables Spring Data JPA Auditing features (@CreatedDate, @LastModifiedDate)
}
