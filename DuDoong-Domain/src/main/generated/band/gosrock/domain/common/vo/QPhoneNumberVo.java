package band.gosrock.domain.common.vo;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPhoneNumberVo is a Querydsl query type for PhoneNumberVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPhoneNumberVo extends BeanPath<PhoneNumberVo> {

    private static final long serialVersionUID = -537816981L;

    public static final QPhoneNumberVo phoneNumberVo = new QPhoneNumberVo("phoneNumberVo");

    public final StringPath internationalFormat = createString("internationalFormat");

    public final StringPath nationalFormat = createString("nationalFormat");

    public final StringPath naverSmsToNumber = createString("naverSmsToNumber");

    public final StringPath phoneNumber = createString("phoneNumber");

    public QPhoneNumberVo(String variable) {
        super(PhoneNumberVo.class, forVariable(variable));
    }

    public QPhoneNumberVo(Path<? extends PhoneNumberVo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPhoneNumberVo(PathMetadata metadata) {
        super(PhoneNumberVo.class, metadata);
    }

}

