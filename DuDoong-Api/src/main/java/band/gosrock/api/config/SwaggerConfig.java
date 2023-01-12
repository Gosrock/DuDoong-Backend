package band.gosrock.api.config;


import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.example.docs.ExampleExceptionDocs;
import band.gosrock.common.annotation.DisableSwaggerSecurity;
import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.dto.ErrorResponse;
import band.gosrock.common.exception.ErrorCode;
import band.gosrock.domain.domains.user.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/** Swagger 사용 환경을 위한 설정 파일 */
// @OpenAPIDefinition(servers = {@io.swagger.v3.oas.annotations.servers.Server(url = "/",
// description = "Default Server URL")})
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final ApplicationContext applicationContext;
    @Bean
    public OpenAPI openAPI(ServletContext servletContext) {
        String contextPath = servletContext.getContextPath();
        Server server = new Server().url(contextPath);
        return new OpenAPI().servers(List.of(server)).components(authSetting()).info(swaggerInfo());
    }

    private Info swaggerInfo() {
        License license = new License();
        license.setUrl("https://github.com/Gosrock/DuDoong-Backend");
        license.setName("두둥");

        return new Info()
                .version("v0.0.1")
                .title("\"두둥 서버 API문서\"")
                .description("두둥 서버의 API 문서 입니다.")
                .license(license);
    }

    private Components authSetting() {
        return new Components()
                .addSecuritySchemes(
                        "access-token",
                        new SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(In.HEADER)
                                .name("Authorization"));
    }

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            DisableSwaggerSecurity methodAnnotation =
                    handlerMethod.getMethodAnnotation(DisableSwaggerSecurity.class);
            List<String> tags = getTags(handlerMethod);
            // DisableSecurity 어노테이션있을시 스웨거 시큐리티 설정 삭제
            if (methodAnnotation != null) {
                operation.setSecurity(Collections.emptyList());
            }
            // 태그 중복 설정시 제일 구체적인 값만 태그로 설정
            if (!tags.isEmpty()) {
                operation.setTags(Collections.singletonList(tags.get(0)));
            }
            generateErrorResponseExample(operation);

            return operation;
        };
    }

    private void generateErrorResponseExample(Operation operation) {
        ApiResponses responses = operation.getResponses();
        Content content = new Content();
        MediaType mediaType = new MediaType();
        ApiResponse apiResponse = new ApiResponse();
        //----------------생성

        ExampleExceptionDocs bean = applicationContext.getBean(ExampleExceptionDocs.class);
        ErrorReason errorReason = UserNotFoundException.EXCEPTION.getErrorCode().getErrorReason();
        ErrorResponse errorResponse = new ErrorResponse(errorReason, "요청시 패스정보입니다.");

        for (Field field: bean.getClass().getDeclaredFields()) {
            ExplainError explainError = field.getAnnotation(ExplainError.class);
            if(explainError != null){
                Example example = new Example();
                example.description(explainError.value());
                example.setValue(errorResponse);
                mediaType.addExamples(UserNotFoundException.class.getSimpleName(),example);

                Example example2 = new Example();
                example2.description(explainError.value());
                example2.setValue(errorResponse);
                mediaType.addExamples("다른 에러",example);
            }
        }


        // -------------------------- 콘텐츠 세팅
        content.addMediaType("application/json",mediaType);
        apiResponse.setContent(content);


        responses.addApiResponse("400",apiResponse);
    }


    private static List<String> getTags(HandlerMethod handlerMethod) {
        List<String> tags = new ArrayList<>();

        Tag[] methodTags = handlerMethod.getMethod().getAnnotationsByType(Tag.class);
        List<String> methodTagStrings =
                Arrays.stream(methodTags).map(Tag::name).collect(Collectors.toList());

        Tag[] classTags = handlerMethod.getClass().getAnnotationsByType(Tag.class);
        List<String> classTagStrings =
                Arrays.stream(classTags).map(Tag::name).collect(Collectors.toList());
        tags.addAll(methodTagStrings);
        tags.addAll(classTagStrings);
        return tags;
    }
}
