package band.gosrock.domain.domains.cart.policy;

import band.gosrock.common.annotation.Policy;
import band.gosrock.domain.domains.cart.exception.CartInvalidItemKindPolicyException;

/**
 * 디폴트 장바구니 관련 정책
 */
@Policy
public class CartPolicyImpl implements CartPolicy{

    /**
     * 아이템 수량이 몇개 까지 가능한 종류인지.
     */
    public void itemKindAvailableQuantity(int quantity){
        if(quantity != 1){
            throw CartInvalidItemKindPolicyException.EXCEPTION;
        }
    }
}
