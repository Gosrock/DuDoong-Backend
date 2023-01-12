package band.gosrock.api.example.controller;


import band.gosrock.api.example.docs.ExampleException2Docs;
import band.gosrock.api.example.docs.ExampleExceptionDocs;
import band.gosrock.api.example.dto.ExampleResponse;
import band.gosrock.api.example.service.ExampleApiService;
import band.gosrock.common.annotation.ApiErrorExample;
import band.gosrock.common.annotation.DevelopOnlyApi;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "access-token")
public class ExampleController {

    private final ExampleApiService exampleApiService;

    @GetMapping
    @DevelopOnlyApi
    @ApiErrorExample(ExampleExceptionDocs.class)
    public ExampleResponse get() {
        return exampleApiService.getExample();
    }

    @PostMapping
    @ApiErrorExample(ExampleException2Docs.class)
    public ExampleResponse create() {
        return exampleApiService.createExample();
    }
}
