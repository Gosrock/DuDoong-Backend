package band.gosrock.domain.example.repository;


import band.gosrock.domain.example.domain.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<ExampleEntity, Long> {}
