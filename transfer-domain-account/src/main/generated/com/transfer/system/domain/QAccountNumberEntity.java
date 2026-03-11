package com.transfer.system.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccountNumberEntity is a Querydsl query type for AccountNumberEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccountNumberEntity extends EntityPathBase<AccountNumberEntity> {

    private static final long serialVersionUID = -2062471304L;

    public static final QAccountNumberEntity accountNumberEntity = new QAccountNumberEntity("accountNumberEntity");

    public final DatePath<java.time.LocalDate> id = createDate("id", java.time.LocalDate.class);

    public final NumberPath<Long> lastNumber = createNumber("lastNumber", Long.class);

    public QAccountNumberEntity(String variable) {
        super(AccountNumberEntity.class, forVariable(variable));
    }

    public QAccountNumberEntity(Path<? extends AccountNumberEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccountNumberEntity(PathMetadata metadata) {
        super(AccountNumberEntity.class, metadata);
    }

}

