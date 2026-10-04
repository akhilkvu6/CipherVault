package com.ciphervault.ciphervault.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.usedStorage = u.usedStorage + :delta WHERE u.id = :userId AND (u.usedStorage + :delta) <= u.storageLimit")
    int incrementStorageUsedAtomic(@Param("userId") Long userId, @Param("delta") Long delta);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.usedStorage = CASE WHEN (u.usedStorage - :delta) < 0 THEN 0 ELSE (u.usedStorage - :delta) END WHERE u.id = :userId")
    int decrementStorageUsedAtomic(@Param("userId") Long userId, @Param("delta") Long delta);
}