package band.gosrock.domain.domains.order.repository.condition;


import lombok.Builder;
import lombok.Getter;

@Getter
public class FindMyPageOrderCondition {
    private final Long userId;
    //
    private final Boolean showing;

    @Builder
    public FindMyPageOrderCondition(Long userId, Boolean showing) {
        this.userId = userId;
        this.showing = showing;
    }

    public static FindMyPageOrderCondition onShowing(Long userId) {
        return FindMyPageOrderCondition.builder().showing(Boolean.TRUE).userId(userId).build();
    }

    public static FindMyPageOrderCondition notShowing(Long userId) {
        return FindMyPageOrderCondition.builder().showing(Boolean.FALSE).userId(userId).build();
    }
}
