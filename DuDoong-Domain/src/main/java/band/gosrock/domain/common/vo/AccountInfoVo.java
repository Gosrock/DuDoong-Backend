package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountInfoVo {

    private final String bankName;

    private final String accountNumber;

    private final String accountHolder;

    public static AccountInfoVo from(TicketItem ticketItem) {
        return AccountInfoVo.builder()
                .bankName(ticketItem.getBankName())
                .accountNumber(ticketItem.getAccountNumber())
                .accountHolder(ticketItem.getAccountHolder())
                .build();
    }
}
