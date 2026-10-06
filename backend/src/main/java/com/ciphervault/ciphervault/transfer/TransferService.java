package com.ciphervault.ciphervault.transfer;

import com.ciphervault.ciphervault.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransferService {

    private final TransferRepository repository;

    public TransferService(TransferRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer initiateTransfer(User user, TransferType type, String fileNames, Long totalBytes, String batchIdentifier) {
        if (user == null) return null;
        
        Transfer transfer = new Transfer();
        transfer.setUser(user);
        transfer.setTransferType(type);
        transfer.setStatus(TransferStatus.PROCESSING);
        transfer.setFileNames(fileNames);
        transfer.setTotalBytes(totalBytes);
        transfer.setTransferredBytes(0L);
        transfer.setBatchIdentifier(batchIdentifier);
        
        return repository.save(transfer);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeTransfer(Long id, Long finalBytes) {
        repository.findById(id).ifPresent(transfer -> {
            transfer.setStatus(TransferStatus.COMPLETED);
            if (finalBytes != null) {
                transfer.setTransferredBytes(finalBytes);
            }
            transfer.setCompletedAt(LocalDateTime.now());
            repository.save(transfer);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failTransfer(Long id, String errorMessage) {
        repository.findById(id).ifPresent(transfer -> {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setErrorMessage(errorMessage);
            transfer.setCompletedAt(LocalDateTime.now());
            repository.save(transfer);
        });
    }
}
