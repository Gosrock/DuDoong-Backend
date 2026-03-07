package band.gosrock.domain.common.vo;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QImageVo is a Querydsl query type for ImageVo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QImageVo extends BeanPath<ImageVo> {

    private static final long serialVersionUID = -1836980561L;

    public static final QImageVo imageVo = new QImageVo("imageVo");

    public final StringPath imageKey = createString("imageKey");

    public QImageVo(String variable) {
        super(ImageVo.class, forVariable(variable));
    }

    public QImageVo(Path<? extends ImageVo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QImageVo(PathMetadata metadata) {
        super(ImageVo.class, metadata);
    }

}

