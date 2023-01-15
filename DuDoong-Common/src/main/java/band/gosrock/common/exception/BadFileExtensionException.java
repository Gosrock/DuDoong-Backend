package band.gosrock.common.exception;

public class BadFileExtensionException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new BadFileExtensionException();

    private BadFileExtensionException() {
        super(GlobalErrorCode.BAD_FILE_EXTENSION);
    }
}
