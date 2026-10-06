package com.ciphervault.ciphervault.transfer;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    
    Page<Transfer> findByUser(User user, Pageable pageable);
    
    Page<Transfer> findByUserAndTransferType(User user, TransferType transferType, Pageable pageable);
    
    Page<Transfer> findByUserAndStatus(User user, TransferStatus status, Pageable pageable);
    
    Optional<Transfer> findByIdAndUser(Long id, User user);
    
    void deleteByUser(User user);
}
