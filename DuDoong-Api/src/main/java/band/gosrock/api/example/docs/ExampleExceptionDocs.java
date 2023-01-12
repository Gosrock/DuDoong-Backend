package band.gosrock.api.example.docs;

import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.domain.domains.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ExampleExceptionDocs {

    @ExplainError("유저검색시에 안나올 때 나오는 에러입니다.")
    private final DuDoongCodeException userNotFoundException = UserNotFoundException.EXCEPTION;
}
