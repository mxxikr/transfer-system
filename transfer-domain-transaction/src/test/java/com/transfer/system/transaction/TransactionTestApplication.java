package com.transfer.system.transaction;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.transfer.system")
@EnableJpaAuditing
@EntityScan("com.transfer.system.domain")
@EnableJpaRepositories("com.transfer.system.repository")
public class TransactionTestApplication {
}
