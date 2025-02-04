package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {

    boolean existsByName(String name);

    List<Lab> findByCreatedBy(User currentUser);

    @Query("SELECT l FROM Lab l JOIN FETCH l.members WHERE l.id = :id")
    Optional<Lab> findLabWithMembers(@Param("id") long id);

    Optional<Lab> findById(Long id);


    Optional<Lab> findByMembers(User user);

    @Transactional
    @Query("SELECT l FROM Lab l JOIN l.members m WHERE m.id = :userId")
    Set<Lab> findLabsByUserId(@Param("userId") Long userId);

}

