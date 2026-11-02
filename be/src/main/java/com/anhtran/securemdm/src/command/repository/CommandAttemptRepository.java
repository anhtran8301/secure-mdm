package com.anhtran.securemdm.src.command.repository;

import com.anhtran.securemdm.src.command.entity.CommandAttempt;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CommandAttemptRepository extends ReactiveCrudRepository<CommandAttempt, String> {}
