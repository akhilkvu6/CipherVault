package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.activity.ActivityEvent;
import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.transfer.Transfer;
import com.ciphervault.ciphervault.transfer.TransferRepository;
import com.ciphervault.ciphervault.transfer.TransferStatus;
import com.ciphervault.ciphervault.transfer.TransferType;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.FileStorageService;
import com.ciphervault.ciphervault.file.StoredFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserDeletionIntegrationTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    @Transactional
    public void testDeleteAccountWithDependencies() throws Exception {
        long ts = System.nanoTime();
        // 1. Create a user
        User user = new User();
        user.setEmail("delete_me_" + ts + "@example.com");
        user.setUsername("delete_me_" + ts);
        user.setPassword("hashed_pass");
        user.setTokenVersion(1);
        user = userRepository.save(user);

        // 2. Add an activity event that belongs to the user
        ActivityEvent activityEvent = new ActivityEvent();
        activityEvent.setUser(user);
        activityEvent.setEventType(EventType.UPLOAD);
        activityEvent.setTimestamp(LocalDateTime.now());
        activityEventRepository.save(activityEvent);

        // 3. Add a transfer that belongs to the user
        Transfer transfer = new Transfer();
        transfer.setUser(user);
        transfer.setTransferType(TransferType.UPLOAD);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setStartedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        // 4. Add a stored file with a real temporary physical file in ENCRYPTED_STORAGE
        Path tempEncFile = com.ciphervault.ciphervault.file.FileStorageConfig.ENCRYPTED_STORAGE.resolve("cv_del_test_" + ts + ".enc");
        Files.writeString(tempEncFile, "encrypted test data payload");

        StoredFile storedFile = new StoredFile();
        storedFile.setUser(user);
        storedFile.setOriginalFilename("test_del.txt");
        storedFile.setStoredFilename(tempEncFile.getFileName().toString());
        storedFile.setStoragePath("storage/encrypted/" + tempEncFile.getFileName().toString());
        storedFile.setFileSize(26L);
        storedFile.setContentType("text/plain");
        storedFile.setSha256Hash("dummy_hash_del_" + ts);
        storedFile.setEncrypted(true);
        storedFile = fileRepository.save(storedFile);

        // Verify they were saved
        assertTrue(activityEventRepository.findById(activityEvent.getId()).isPresent());
        assertTrue(transferRepository.findById(transfer.getId()).isPresent());
        assertTrue(fileRepository.findById(storedFile.getId()).isPresent());
        assertTrue(Files.exists(tempEncFile));

        // 5. Authenticate as this user
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), 
                null, 
                java.util.Collections.emptyList()
        );

        // 6. Call the API to delete the account
        ResponseEntity<Map<String, Object>> response = userController.deleteAccount(auth);

        // 7. Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Ensure user is deleted
        Optional<User> deletedUser = userRepository.findById(user.getId());
        assertFalse(deletedUser.isPresent(), "User should be deleted from the database");
        
        // Ensure cascading deletions actually worked and no orphaned records remain
        assertFalse(activityEventRepository.findById(activityEvent.getId()).isPresent(), "Activity event should be deleted");
        assertFalse(transferRepository.findById(transfer.getId()).isPresent(), "Transfer should be deleted");
        assertFalse(fileRepository.findById(storedFile.getId()).isPresent(), "File metadata should be deleted");
        assertFalse(Files.exists(tempEncFile), "Encrypted file should be deleted from disk");
    }

    @Test
    @Transactional
    public void testDeleteAllDataWithDependencies() throws Exception {
        long ts = System.nanoTime();
        // 1. Create a user
        User user = new User();
        user.setEmail("delete_data_me_" + ts + "@example.com");
        user.setUsername("delete_data_me_" + ts);
        user.setPassword("hashed_pass");
        user.setTokenVersion(1);
        user.setUsedStorage(1024L);
        user = userRepository.save(user);

        // 2. Add an activity event that belongs to the user
        ActivityEvent activityEvent = new ActivityEvent();
        activityEvent.setUser(user);
        activityEvent.setEventType(EventType.UPLOAD);
        activityEvent.setTimestamp(LocalDateTime.now());
        activityEventRepository.save(activityEvent);

        // 3. Add a transfer that belongs to the user
        Transfer transfer = new Transfer();
        transfer.setUser(user);
        transfer.setTransferType(TransferType.UPLOAD);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setStartedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        // 4. Add a stored file with a real physical file
        Path tempEncFile = com.ciphervault.ciphervault.file.FileStorageConfig.ENCRYPTED_STORAGE.resolve("cv_del_data_test_" + ts + ".enc");
        Files.writeString(tempEncFile, "encrypted data payload");

        StoredFile storedFile = new StoredFile();
        storedFile.setUser(user);
        storedFile.setOriginalFilename("test_del_data.txt");
        storedFile.setStoredFilename(tempEncFile.getFileName().toString());
        storedFile.setStoragePath("storage/encrypted/" + tempEncFile.getFileName().toString());
        storedFile.setFileSize(22L);
        storedFile.setContentType("text/plain");
        storedFile.setSha256Hash("dummy_hash_del_data_" + ts);
        storedFile.setEncrypted(true);
        storedFile = fileRepository.save(storedFile);

        // 5. Authenticate as this user
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), 
                null, 
                java.util.Collections.emptyList()
        );

        // 6. Call the API to delete ALL DATA
        ResponseEntity<Map<String, Object>> response = userController.deleteAllData(auth);

        // 7. Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Ensure user IS NOT deleted and quota is reset
        Optional<User> keptUser = userRepository.findById(user.getId());
        assertTrue(keptUser.isPresent(), "User should be KEPT in the database");
        assertEquals(0L, keptUser.get().getUsedStorage(), "Quota used storage should be reset to 0");
        
        // Ensure cascading deletions actually worked
        assertFalse(activityEventRepository.findById(activityEvent.getId()).isPresent(), "Activity event should be deleted");
        assertFalse(transferRepository.findById(transfer.getId()).isPresent(), "Transfer should be deleted");
        assertFalse(fileRepository.findById(storedFile.getId()).isPresent(), "File metadata should be deleted");
        assertFalse(Files.exists(tempEncFile), "Encrypted file should be deleted from disk");
    }

    @Test
    public void testConcurrentAccountDeletionLeavesNoOrphans() throws Exception {
        long ts = System.nanoTime();
        User user = new User();
        user.setEmail("concurrent_del_" + ts + "@example.com");
        user.setUsername("concurrent_del_" + ts);
        user.setPassword("hashed_pass");
        user.setTokenVersion(1);
        user = userRepository.save(user);

        Path tempEncFile = com.ciphervault.ciphervault.file.FileStorageConfig.ENCRYPTED_STORAGE.resolve("cv_concur_del_" + ts + ".enc");
        Files.writeString(tempEncFile, "concurrent payload");

        StoredFile storedFile = new StoredFile();
        storedFile.setUser(user);
        storedFile.setOriginalFilename("concurrent_file.txt");
        storedFile.setStoredFilename(tempEncFile.getFileName().toString());
        storedFile.setStoragePath("storage/encrypted/" + tempEncFile.getFileName().toString());
        storedFile.setFileSize(18L);
        storedFile.setContentType("text/plain");
        storedFile.setSha256Hash("dummy_concur_hash_" + ts);
        storedFile.setEncrypted(true);
        storedFile = fileRepository.save(storedFile);

        ActivityEvent event = new ActivityEvent();
        event.setUser(user);
        event.setEventType(EventType.UPLOAD);
        event.setTimestamp(LocalDateTime.now());
        activityEventRepository.save(event);

        Transfer transfer = new Transfer();
        transfer.setUser(user);
        transfer.setTransferType(TransferType.UPLOAD);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setStartedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, java.util.Collections.emptyList()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<ResponseEntity<Map<String, Object>>> f1 = executor.submit(() -> userController.deleteAccount(auth));
        Future<ResponseEntity<Map<String, Object>>> f2 = executor.submit(() -> userController.deleteAccount(auth));

        ResponseEntity<Map<String, Object>> r1 = f1.get(10, TimeUnit.SECONDS);
        ResponseEntity<Map<String, Object>> r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // At least one request must return 200 OK
        assertTrue(r1.getStatusCode().is2xxSuccessful() || r2.getStatusCode().is2xxSuccessful());

        // Verify final state: zero orphaned files, zero orphaned records
        assertFalse(userRepository.findById(user.getId()).isPresent(), "User should be completely deleted");
        assertFalse(fileRepository.findById(storedFile.getId()).isPresent(), "File metadata should be completely deleted");
        assertFalse(Files.exists(tempEncFile), "Encrypted file should be deleted from disk");
        assertFalse(activityEventRepository.findById(event.getId()).isPresent(), "Activity event should be completely deleted");
        assertFalse(transferRepository.findById(transfer.getId()).isPresent(), "Transfer should be deleted");
    }
}
