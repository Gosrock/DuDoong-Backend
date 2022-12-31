package band.gosrock.api.example.service;


import band.gosrock.api.example.dto.ExampleResponse;
import band.gosrock.domain.example.domain.ExampleEntity;
import band.gosrock.domain.example.service.ExampleDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExampleApiService {

    private final ExampleDomainService exampleDomainService;

    public ExampleResponse getExample() {
        ExampleEntity query = exampleDomainService.query(1L);

        return ExampleResponse.from(query);
    }

    public ExampleResponse createExample() {
        ExampleEntity asdf = exampleDomainService.save("asdf");
        return ExampleResponse.from(asdf);
    }
}
