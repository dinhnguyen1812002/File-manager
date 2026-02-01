package com.app.file_transfer.repository;

import com.app.file_transfer.model.PersistentLogins;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersistentLoginRepository extends JpaRepository<PersistentLogins, String> {

    void deleteByUsername(String username);
}
