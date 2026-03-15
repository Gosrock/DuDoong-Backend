package band.gosrock.api.config

import io.swagger.v3.oas.models.examples.Example

data class ExampleHolder(
    val holder: Example,
    val name: String,
    val code: Int,
)
