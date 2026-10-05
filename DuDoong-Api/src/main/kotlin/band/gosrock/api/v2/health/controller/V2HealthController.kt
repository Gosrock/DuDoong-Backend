package band.gosrock.api.v2.health.controller

import band.gosrock.api.v2.common.swagger.V2ApiTags
import band.gosrock.api.v2.health.dto.V2HealthResponse
import band.gosrock.common.annotation.DisableSwaggerSecurity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/health")
@Tag(name = V2ApiTags.HEALTH, description = V2ApiTags.HEALTH_DESCRIPTION)
class V2HealthController {

    @GetMapping
    @DisableSwaggerSecurity
    @Operation(summary = "v2 헬스체크 (공개)")
    fun health(): V2HealthResponse = V2HealthResponse(status = "UP")
}
