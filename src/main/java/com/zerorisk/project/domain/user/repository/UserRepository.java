package com.zerorisk.project.domain.user.repository;

import com.zerorisk.project.domain.user.entity.OAuthProvider;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByOauthProviderAndOauthProviderId(OAuthProvider oauthProvider, String oauthProviderId);

    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR u.email LIKE CONCAT('%', :keyword, '%') OR u.nickname LIKE CONCAT('%', :keyword, '%'))
            AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}