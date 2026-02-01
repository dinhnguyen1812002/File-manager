package com.app.file_transfer.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "persistent_logins")
@Getter
@Setter
public class PersistentLogins {

    @Id
    @Column(name = "series", length = 64)
    private String series;

    @Column(name = "username", length = 64, nullable = false)
    private String username;

    @Column(name = "token", length = 64, nullable = false)
    private String token;

    @Column(name = "last_used", nullable = false)

    private LocalDateTime lastUsed;

    // Constructors
    public PersistentLogins() {}

    public PersistentLogins(String series, String username, String token, LocalDateTime lastUsed) {
        this.series = series;
        this.username = username;
        this.token = token;
        this.lastUsed = lastUsed;
    }
}