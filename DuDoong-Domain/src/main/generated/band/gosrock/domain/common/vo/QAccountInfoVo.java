package band.gosrock.domain.common.vo;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccountInfoVo is a Querydsl query type for AccountInfoVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QAccountInfoVo extends BeanPath<AccountInfoVo> {

    private static final long serialVersionUID = -2011274865L;

    public static final QAccountInfoVo accountInfoVo = new QAccountInfoVo("accountInfoVo");

    public final StringPath accountHolder = createString("accountHolder");

    public final StringPath accountNumber = createString("accountNumber");

    public final StringPath bankName = createString("bankName");

    public QAccountInfoVo(String variable) {
        super(AccountInfoVo.class, forVariable(variable));
    }

    public QAccountInfoVo(Path<? extends AccountInfoVo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccountInfoVo(PathMetadata metadata) {
        super(AccountInfoVo.class, metadata);
    }

}

