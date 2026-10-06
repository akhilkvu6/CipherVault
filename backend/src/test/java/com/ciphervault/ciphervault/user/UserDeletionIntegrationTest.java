package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.activity.ActivityEvent;
import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.transfer.Transfer;
import com.ciphervault.ciphervault.transfer.TransferRepository;
import com.ciphervault.ciphervault.transfer.TransferStatus;
import com.ciphervault.ciphervault.transfer.TransferType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    @Test
    @Transactional
    public void testDeleteAccountWithDependencies() {
        // 1. Create a user
        User user = new User();
        user.setEmail("delete_me@example.com");
        user.setUsername("delete_me");
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

        // Verify they were saved
        assertTrue(activityEventRepository.findById(activityEvent.getId()).isPresent());
        assertTrue(transferRepository.findById(transfer.getId()).isPresent());

        // 4. Authenticate as this user
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), 
                null, 
                java.util.Collections.emptyList()
        );

        // 5. Call the API to delete the account
        ResponseEntity<Map<String, Object>> response = userController.deleteAccount(auth);

        // 6. Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Ensure user is deleted
        Optional<User> deletedUser = userRepository.findById(user.getId());
        assertFalse(deletedUser.isPresent(), "User should be deleted from the database");
        
        // Ensure cascading deletions actually worked and didn't throw ConstraintViolationException
        assertFalse(activityEventRepository.findById(activityEvent.getId()).isPresent(), "Activity event should be deleted");
        assertFalse(transferRepository.findById(transfer.getId()).isPresent(), "Transfer should be deleted");
    }

    @Test
    @Transactional
    public void testDeleteAllDataWithDependencies() {
        // 1. Create a user
        User user = new User();
        user.setEmail("delete_data_me@example.com");
        user.setUsername("delete_data_me");
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

        // 4. Authenticate as this user
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), 
                null, 
                java.util.Collections.emptyList()
        );

        // 5. Call the API to delete ALL DATA
        ResponseEntity<Map<String, Object>> response = userController.deleteAllData(auth);

        // 6. Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Ensure user IS NOT deleted
        Optional<User> keptUser = userRepository.findById(user.getId());
        assertTrue(keptUser.isPresent(), "User should be KEPT in the database");
        
        // Ensure cascading deletions actually worked
        assertFalse(activityEventRepository.findById(activityEvent.getId()).isPresent(), "Activity event should be deleted");
        assertFalse(transferRepository.findById(transfer.getId()).isPresent(), "Transfer should be deleted");
    }
}
