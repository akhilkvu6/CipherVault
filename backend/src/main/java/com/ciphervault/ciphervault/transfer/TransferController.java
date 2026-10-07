package com.ciphervault.ciphervault.transfer;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferRepository repository;
    private final UserRepository userRepository;

    public TransferController(TransferRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> listTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) TransferType type,
            @RequestParam(required = false) TransferStatus status,
            Authentication authentication) {
            
        User user = getAuthenticatedUser(authentication);
        size = Math.max(1, Math.min(size, 100)); page = Math.max(0, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        
        Page<Transfer> transfers;
        if (type != null) {
            transfers = repository.findByUserAndTransferType(user, type, pageable);
        } else if (status != null) {
            transfers = repository.findByUserAndStatus(user, status, pageable);
        } else {
            transfers = repository.findByUser(user, pageable);
        }
        
        return ResponseEntity.ok(transfers.map(this::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTransferDetail(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return repository.findByIdAndUser(id, user)
                .map(t -> ResponseEntity.ok(toDto(t)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Transfer not found"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTransfer(@PathVariable Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Transfer transfer = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Transfer not found"));
        
        if (transfer.getStatus() == TransferStatus.COMPLETED || transfer.getStatus() == TransferStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Transfer is already finished");
        }
        
        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setUpdatedAt(LocalDateTime.now());
        repository.save(transfer);
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Transfer cancelled successfully"));
    }

    private Map<String, Object> toDto(Transfer t) {
        double progress = 0.0;
        long speed = 0;
        long eta = 0;
        if (t.getTotalBytes() != null && t.getTotalBytes() > 0 && t.getTransferredBytes() != null) {
            progress = ((double) t.getTransferredBytes() / t.getTotalBytes()) * 100;
        }
        if (t.getTransferredBytes() != null && t.getTransferredBytes() > 0) {
            LocalDateTime end = t.getCompletedAt() != null ? t.getCompletedAt() : LocalDateTime.now();
            long seconds = Duration.between(t.getStartedAt(), end).getSeconds();
            if (seconds > 0) {
                speed = t.getTransferredBytes() / seconds;
                if (t.getTotalBytes() != null) {
                    long remainingBytes = Math.max(0, t.getTotalBytes() - t.getTransferredBytes());
                    eta = remainingBytes / speed;
                }
            }
        }
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("filename", t.getFileNames() != null ? t.getFileNames() : "File transfer");
        map.put("fileNames", t.getFileNames() != null ? t.getFileNames() : "File transfer");
        map.put("type", t.getTransferType() != null ? t.getTransferType().name() : "DOWNLOAD");
        map.put("transferType", t.getTransferType() != null ? t.getTransferType().name() : "DOWNLOAD");
        map.put("status", t.getStatus() != null ? t.getStatus().name() : "COMPLETED");
        map.put("totalBytes", t.getTotalBytes() != null ? t.getTotalBytes() : 0L);
        map.put("bytesTransferred", t.getTransferredBytes() != null ? t.getTransferredBytes() : 0L);
        map.put("transferredBytes", t.getTransferredBytes() != null ? t.getTransferredBytes() : 0L);
        map.put("progressPercentage", progress);
        map.put("speedBytesPerSecond", speed);
        map.put("estimatedRemainingSeconds", eta);
        map.put("startedAt", t.getStartedAt() != null ? t.getStartedAt().toString() : "");
        map.put("updatedAt", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "");
        return map;
    }
}

