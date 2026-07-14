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

    long countByRolesNameAndCreatedBy(String roleName, User createdBy);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.createdBy = :createdBy AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRolesNameAndCreatedByAndCreatedAtBetween(@Param("roleName") String roleName, @Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.labs l WHERE r.name = :roleName AND l.id = :labId")
    long countByRolesNameAndLabsId(@Param("roleName") String roleName, @Param("labId") Long labId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r JOIN u.labs l WHERE r.name = :roleName AND l.id = :labId AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByRolesNameAndLabsIdAndCreatedAtBetween(@Param("roleName") String roleName, @Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}