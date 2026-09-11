package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findByUser(User user);

    Optional<StoredFile> findByIdAndUser(Long id, User user);

    boolean existsByUserAndSha256Hash(User user, String sha256Hash);

    Optional<StoredFile> findByUserAndSha256Hash(User user, String sha256Hash);
}