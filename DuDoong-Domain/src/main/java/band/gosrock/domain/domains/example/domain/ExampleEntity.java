package band.gosrock.domain.domains.example.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "tbl_example")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExampleEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Builder
    public ExampleEntity(String content) {
        this.content = content;
    }
}
