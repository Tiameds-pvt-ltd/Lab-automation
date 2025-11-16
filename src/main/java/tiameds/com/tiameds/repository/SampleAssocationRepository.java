package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.SampleEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SampleAssocationRepository extends JpaRepository<SampleEntity, Long> {

    Optional<SampleEntity> findByName(String name);

    List<SampleEntity> findByLabId(Long labId);

    Optional<SampleEntity> findByNameAndLabId(String name, Long labId);

}

