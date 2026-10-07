package com.ciphervault.ciphervault.transfer;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    
    Page<Transfer> findByUser(User user, Pageable pageable);
    
    Page<Transfer> findByUserAndTransferType(User user, TransferType transferType, Pageable pageable);
    
    Page<Transfer> findByUserAndStatus(User user, TransferStatus status, Pageable pageable);
    
    Optional<Transfer> findByIdAndUser(Long id, User user);
    
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Transfer t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);
}
