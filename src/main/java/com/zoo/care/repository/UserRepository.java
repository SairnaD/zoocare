package com.zoo.care.repository;

import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByRoleAndDeletedStatusOrderByFullName(Role role, DeletedStatus deletedStatus);
}