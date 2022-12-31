package band.gosrock.domain.example.service;


import band.gosrock.common.exception.DuDoongException;
import band.gosrock.domain.example.domain.ExampleEntity;
import band.gosrock.domain.example.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExampleDomainService {

    private final ExampleRepository exampleRepository;

    public void exception() {
        throw new DuDoongException();
    }

    public ExampleEntity query(Long id) {
        return exampleRepository.findById(id).orElseThrow(DuDoongException::new);
    }

    public ExampleEntity save(String content) {
        ExampleEntity build = ExampleEntity.builder().content(content).build();
        return exampleRepository.save(build);
    }
}
