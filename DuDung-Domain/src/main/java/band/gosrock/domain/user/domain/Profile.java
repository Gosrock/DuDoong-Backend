package band.gosrock.domain.user.domain;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    private String name;
    private String email;

    @Builder
    public Profile(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
