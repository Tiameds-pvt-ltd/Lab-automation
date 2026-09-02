package tiameds.com.tiameds.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    // ── Batch role-count per lab (replaces the N+1 loop in buildRoleLabWise) ──

    /**
     * One query instead of N: returns (labId, labName, count) for every lab owned
     * by {@code createdBy} that has at least one enabled member with {@code roleName}.
     * Labs with zero members for that role are NOT included — caller treats missing
     * labs as count=0.
     */
    @Query("SELECT l.id AS labId, l.name AS labName, COUNT(u.id) AS count " +
           "FROM Lab l JOIN l.members u JOIN u.roles r " +
           "WHERE l.createdBy = :createdBy AND r.name = :roleName AND u.enabled = true " +
           "GROUP BY l.id, l.name")
    List<LabRoleCountProjection> countRolesByLabsCreatedBy(
            @Param("createdBy") User createdBy,
            @Param("roleName") String roleName);

    @Query("SELECT l.id AS labId, l.name AS labName, COUNT(u.id) AS count " +
           "FROM Lab l JOIN l.members u JOIN u.roles r " +
           "WHERE l.createdBy = :createdBy AND r.name = :roleName AND u.enabled = true " +
           "AND u.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY l.id, l.name")
    List<LabRoleCountProjection> countRolesByLabsCreatedByAndCreatedAtBetween(
            @Param("createdBy") User createdBy,
            @Param("roleName") String roleName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    interface LabRoleCountProjection {
        Long getLabId();
        String getLabName();
        Long getCount();
    }



    @Query("SELECT u FROM User u WHERE u.username = :username")
    public User getUserByUsername(@Param("username") String username);
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    @NotNull Optional<User> findById(@NotNull Long id);

    List<User> findByCreatedBy(User createdBy);

    List<User> findByLabsId(Long labId);

    boolean existsByIdAndLabsId(Long userId, Long labId);

    long countByRolesName(String roleName);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.createdBy = :createdBy AND u.enabled = true")
    long countByRolesNameAndCreatedBy(@Param("roleName") String roleName, @Param("createdBy") User createdBy);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.createdBy = :createdBy AND u.enabled = true AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRolesNameAndCreatedByAndCreatedAtBetween(@Param("roleName") String roleName, @Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.labs l WHERE r.name = :roleName AND l.id = :labId AND u.enabled = true")
    long countByRolesNameAndLabsId(@Param("roleName") String roleName, @Param("labId") Long labId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.labs l WHERE r.name = :roleName AND l.id = :labId AND u.enabled = true AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRolesNameAndLabsIdAndCreatedAtBetween(@Param("roleName") String roleName, @Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}