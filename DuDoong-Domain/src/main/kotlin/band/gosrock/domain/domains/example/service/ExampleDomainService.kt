package band.gosrock.domain.domains.example.service

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.domain.domains.example.domain.ExampleEntity
import band.gosrock.domain.domains.example.repository.ExampleRepository
import org.springframework.stereotype.Service

@Service
class ExampleDomainService(
    private val exampleRepository: ExampleRepository
) {

    fun exception() {
        throw DuDoongDynamicException(400, "에러코드", "메세지")
    }

    fun query(id: Long): ExampleEntity {
        return exampleRepository.findById(id)
            .orElseThrow { DuDoongDynamicException(400, "에러코드", "메세지") }
    }

    fun save(content: String): ExampleEntity {
        val entity = ExampleEntity(content)
        return exampleRepository.save(entity)
    }
}
