package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository
    extends ReactiveCrudRepository<User, String>, UserRepositoryCustom {}
