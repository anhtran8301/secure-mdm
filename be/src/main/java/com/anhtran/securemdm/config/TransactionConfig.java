package com.anhtran.securemdm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Services wrap units of work with {@link TransactionalOperator} instead of {@code @Transactional}
 * so a transaction can commit before an error is signalled (see refresh-token reuse).
 */
@Configuration
public class TransactionConfig {
  @Bean
  public TransactionalOperator transactionalOperator(ReactiveTransactionManager manager) {
    return TransactionalOperator.create(manager);
  }
}
