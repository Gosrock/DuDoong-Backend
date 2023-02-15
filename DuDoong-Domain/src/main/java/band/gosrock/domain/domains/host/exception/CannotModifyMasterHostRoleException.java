package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotModifyMasterHostRoleException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new CannotModifyMasterHostRoleException();

    private CannotModifyMasterHostRoleException() {
        super(HostErrorCode.CANNOT_MODIFY_MASTER_HOST_ROLE);
    }
}
