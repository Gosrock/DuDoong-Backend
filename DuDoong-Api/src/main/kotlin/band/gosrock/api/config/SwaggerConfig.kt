package band.gosrock.api.config

import band.gosrock.common.annotation.ApiErrorCodeExample
import band.gosrock.common.annotation.ApiErrorExceptionsExample
import band.gosrock.common.annotation.DisableSwaggerSecurity
import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.BaseErrorCode
import band.gosrock.common.exception.DuDoongCodeException
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import java.util.Collections
import jakarta.servlet.ServletContext

/** Swagger 사용 환경을 위한 설정 파일 */
@Configuration
@org.springframework.context.annotation.Profile("!staging", "!prod")
class SwaggerConfig(
    private val applicationContext: ApplicationContext,
) {
    @Bean
    fun openAPI(servletContext: ServletContext): OpenAPI {
        val contextPath = servletContext.contextPath
        val server = Server().url(contextPath)
        return OpenAPI().servers(listOf(server)).components(authSetting()).info(swaggerInfo())
    }

    private fun swaggerInfo(): Info {
        val license = License()
        license.url = "https://github.com/Gosrock/DuDoong-Backend"
        license.name = "두둥"
        return Info()
            .version("v0.0.1")
            .title("\"두둥 서버 API문서\"")
            .description("두둥 서버의 API 문서 입니다.")
            .license(license)
    }

    private fun authSetting(): Components =
        Components()
            .addSecuritySchemes(
                "access-token",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .`in`(SecurityScheme.In.HEADER)
                    .name("Authorization"),
            )
            .addSecuritySchemes(
                "admin-token",
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.HEADER)
                    .name("X-Admin-Token")
                    .description("Admin JWT 토큰 (aud:admin 포함)"),
            )

    @Bean
    fun modelResolver(objectMapper: ObjectMapper): ModelResolver = ModelResolver(objectMapper)

    @Bean
    fun customize(): OperationCustomizer =
        OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
            val methodAnnotation = handlerMethod.getMethodAnnotation(DisableSwaggerSecurity::class.java)
            val apiErrorExceptionsExample = handlerMethod.getMethodAnnotation(ApiErrorExceptionsExample::class.java)
            val apiErrorCodeExample = handlerMethod.getMethodAnnotation(ApiErrorCodeExample::class.java)


            val tags = getTags(handlerMethod)
            // DisableSecurity 어노테이션있을시 스웨거 시큐리티 설정 삭제
            if (methodAnnotation != null) {
                operation.security = Collections.emptyList()
            }
            // 태그 중복 설정시 제일 구체적인 값만 태그로 설정
            if (tags.isNotEmpty()) {
                operation.tags = Collections.singletonList(tags[0])
            }
            // ApiErrorExceptionsExample 어노테이션 단 메소드 적용
            if (apiErrorExceptionsExample != null) {
                generateExceptionResponseExample(operation, apiErrorExceptionsExample.value.java)
            }
            // ApiErrorCodeExample 어노테이션 단 메소드 적용
            if (apiErrorCodeExample != null) {
                generateErrorCodeResponseExample(operation, apiErrorCodeExample.value.java)
            }
            operation
        }

    /**
     * BaseErrorCode 타입의 이넘값들을 문서화 시킵니다. ExplainError 어노테이션으로 부가설명을 붙일수있습니다.
     * 필드들을 가져와서 예시 에러 객체를 동적으로 생성해서 예시값으로 붙입니다.
     */
    private fun generateErrorCodeResponseExample(
        operation: Operation,
        type: Class<out BaseErrorCode>,
    ) {
        val responses = operation.responses
        val errorCodes = type.enumConstants

        val statusWithExampleHolders = errorCodes
            .map { baseErrorCode ->
                val errorReason = baseErrorCode.getErrorReason()
                ExampleHolder(
                    holder = getSwaggerExample(baseErrorCode.getExplainError(), errorReason),
                    code = errorReason.status,
                    name = errorReason.code,
                )
            }
            .groupBy { it.code }

        addExamplesToResponses(responses, statusWithExampleHolders)
    }

    /**
     * SwaggerExampleExceptions 타입의 클래스를 문서화 시킵니다.
     * SwaggerExampleExceptions 타입의 클래스는 필드로 DuDoongCodeException 타입을 가지며,
     * DuDoongCodeException 의 errorReason 와, ExplainError 의 설명을 문서화시킵니다.
     */
    private fun generateExceptionResponseExample(operation: Operation, type: Class<*>) {
        val responses = operation.responses
        val bean = applicationContext.getBean(type)
        val declaredFields = bean.javaClass.declaredFields

        val statusWithExampleHolders = declaredFields
            .filter { field -> field.getAnnotation(ExplainError::class.java) != null }
            .filter { field -> field.type == DuDoongCodeException::class.java }
            .map { field ->
                val exception = field.get(bean) as DuDoongCodeException
                val annotation = field.getAnnotation(ExplainError::class.java)
                val value = annotation.value
                val errorReason = exception.getErrorReason()
                ExampleHolder(
                    holder = getSwaggerExample(value, errorReason),
                    code = errorReason.status,
                    name = field.name,
                )
            }
            .groupBy { it.code }

        addExamplesToResponses(responses, statusWithExampleHolders)
    }

    private fun getSwaggerExample(value: String, errorReason: ErrorReason): Example {
        val errorResponse = ErrorResponse(errorReason, "요청시 패스정보입니다.")
        val example = Example()
        example.description(value)
        example.value = errorResponse
        return example
    }

    private fun addExamplesToResponses(
        responses: ApiResponses,
        statusWithExampleHolders: Map<Int, List<ExampleHolder>>,
    ) {
        statusWithExampleHolders.forEach { (status, exampleHolders) ->
            val content = Content()
            val mediaType = MediaType()
            val apiResponse = ApiResponse()
            exampleHolders.forEach { exampleHolder ->
                mediaType.addExamples(exampleHolder.name, exampleHolder.holder)
            }
            content.addMediaType("application/json", mediaType)
            apiResponse.content = content
            responses.addApiResponse(status.toString(), apiResponse)
        }
    }

    companion object {
        private fun getTags(handlerMethod: HandlerMethod): List<String> {
            val tags = mutableListOf<String>()
            val methodTags = handlerMethod.method.getAnnotationsByType(Tag::class.java)
            val methodTagStrings = methodTags.map { it.name }
            val classTags = handlerMethod.javaClass.getAnnotationsByType(Tag::class.java)
            val classTagStrings = classTags.map { it.name }
            tags.addAll(methodTagStrings)
            tags.addAll(classTagStrings)
            return tags
        }
    }
}
