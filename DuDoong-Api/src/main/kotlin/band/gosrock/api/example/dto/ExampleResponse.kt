package band.gosrock.api.example.dto

import band.gosrock.domain.domains.example.domain.ExampleEntity

data class ExampleResponse(
    val id: Long,
    val content: String,
) {
    companion object {
        @JvmStatic
        fun from(exampleEntity: ExampleEntity): ExampleResponse =
            ExampleResponse(
                id = exampleEntity.id,
                content = exampleEntity.content,
            )
    }
}
