package com.bootsignal.support;

import com.bootsignal.domain.verification.storage.VerificationFileStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestVerificationFileStorageConfig {

    @Bean
    public VerificationFileStorage verificationFileStorage() {
        return new InMemoryVerificationFileStorage();
    }
}
