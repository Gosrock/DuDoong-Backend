package band.gosrock.domain.domains.settlement.domain;


import lombok.Getter;

@Getter
public enum EventSettlementStatus {
    READY,
    CALCULATED,
    ADMIN_CONFIRM,
    CLIENT_CONFIRM
}
