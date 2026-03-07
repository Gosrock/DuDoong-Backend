package band.gosrock.domain.domains.settlement.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSettlementFeeVo is a Querydsl query type for SettlementFeeVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QSettlementFeeVo extends BeanPath<SettlementFeeVo> {

    private static final long serialVersionUID = -1057286913L;

    public static final QSettlementFeeVo settlementFeeVo = new QSettlementFeeVo("settlementFeeVo");

    public final NumberPath<Long> fee = createNumber("fee", Long.class);

    public final EnumPath<band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode> type = createEnum("type", band.gosrock.infrastructure.outer.api.tossPayments.dto.response.FeeCode.class);

    public QSettlementFeeVo(String variable) {
        super(SettlementFeeVo.class, forVariable(variable));
    }

    public QSettlementFeeVo(Path<? extends SettlementFeeVo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSettlementFeeVo(PathMetadata metadata) {
        super(SettlementFeeVo.class, metadata);
    }

}

