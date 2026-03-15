package band.gosrock.domain.domains.example.repository

import band.gosrock.domain.domains.example.domain.ExampleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ExampleRepository : JpaRepository<ExampleEntity, Long>
