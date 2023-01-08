package band.gosrock.api.example.dto;


import band.gosrock.domain.domains.example.domain.ExampleEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExampleResponse {

    private final Long id;
    private final String content;

    public static ExampleResponse from(ExampleEntity exampleEntity) {
        return new ExampleResponse(exampleEntity.getId(), exampleEntity.getContent());
    }
}
