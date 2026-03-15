package band.gosrock.api.example.service

import band.gosrock.api.example.dto.ExampleResponse
import band.gosrock.domain.domains.example.service.ExampleDomainService
import org.springframework.stereotype.Service

@Service
class ExampleApiService(
    private val exampleDomainService: ExampleDomainService,
) {
    fun getExample(): ExampleResponse {
        val query = exampleDomainService.query(1L)
        return ExampleResponse.from(query)
    }

    fun createExample(): ExampleResponse {
        val asdf = exampleDomainService.save("asdf")
        return ExampleResponse.from(asdf)
    }
}
