package band.gosrock.domain.common.vo;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class AccountInfoVo {

    private String bankName;

    private String accountNumber;

    private String accountHolder;

    public AccountInfoVo(String bankName, String accountNumber, String accountHolder) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

    public static AccountInfoVo valueOf(
            String bankName, String accountNumber, String accountHolder) {
        return new AccountInfoVo(bankName, accountNumber, accountHolder);
    }
}
