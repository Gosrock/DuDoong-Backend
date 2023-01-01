package band.gosrock.domain.domains.example.service;


import band.gosrock.common.consts.DuDoongStatic;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.domain.domains.example.domain.ExampleEntity;
import band.gosrock.domain.domains.example.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExampleDomainService {

    private final ExampleRepository exampleRepository;

    public void exception() {
        throw new DuDoongDynamicException(400, "에러코드","메세지");
    }

    public ExampleEntity query(Long id) {
        return exampleRepository.findById(id).orElseThrow(()->new DuDoongDynamicException(400, "에러코드","메세지"));
    }

    public ExampleEntity save(String content) {
        ExampleEntity build = ExampleEntity.builder().content(content).build();
        return exampleRepository.save(build);
    }
}
