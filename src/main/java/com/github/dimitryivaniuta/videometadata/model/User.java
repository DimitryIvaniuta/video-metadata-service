// src/main/java/com/github/dimitryivaniuta/videometadata/model/User.java
package com.github.dimitryivaniuta.videometadata.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {
    @Id
    private Long id;

    private String username;
    private String password;
    private String email;
    /** Now an enum, backed by INT in the DB */
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}
