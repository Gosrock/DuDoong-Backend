package band.gosrock.common.exception;

public class SecurityContextNotFoundException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new SecurityContextNotFoundException();

    private SecurityContextNotFoundException() {
        super(ErrorCode.SECURITY_CONTEXT_NOT_FOUND);
    }
}
