package com.ums.authorization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.authorization.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    @Query(value = "select ur from UserRole ur join fetch ur.role r",
            countQuery = "select count(ur) from UserRole ur")
    Page<UserRole> findAllWithRole(Pageable pageable);

    @Query("""
            select ur from UserRole ur
            join fetch ur.role r
            where ur.id = :assignmentId
            """)
    Optional<UserRole> findByIdWithRole(@Param("assignmentId") UUID assignmentId);

    @Query("""
            select ur from UserRole ur
            join fetch ur.role r
            where ur.userId = :userId
            order by ur.assignedAt desc
            """)
    List<UserRole> findByUserIdWithRole(@Param("userId") UUID userId);

    boolean existsByUserIdAndRole_Id(UUID userId, UUID roleId);

    boolean existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(
            UUID userId, UUID roleId, String scopeType, String scopeId);

    @Query("""
            select ur from UserRole ur
            join fetch ur.role r
            where ur.userId = :userId
              and ur.active = true
              and r.active = true
              and (ur.expiresAt is null or ur.expiresAt > CURRENT_TIMESTAMP)
              and ur.scopeType = 'PLATFORM'
              and ur.scopeId = '*'
            """)
    List<UserRole> findActivePlatformAssignments(@Param("userId") UUID userId);

    @Query("""
            select ur from UserRole ur
            join fetch ur.role r
            where ur.userId = :userId
              and ur.active = true
              and r.active = true
              and (ur.expiresAt is null or ur.expiresAt > CURRENT_TIMESTAMP)
              and (
                    (ur.scopeType = 'PLATFORM' and ur.scopeId = '*')
                    or (ur.scopeType = :scopeType and ur.scopeId = :scopeId)
                  )
            """)
    List<UserRole> findActiveAssignments(
            @Param("userId") UUID userId,
            @Param("scopeType") String scopeType,
            @Param("scopeId") String scopeId);
}
