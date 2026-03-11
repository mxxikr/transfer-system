package com.transfer.system.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccountEntity is a Querydsl query type for AccountEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccountEntity extends EntityPathBase<AccountEntity> {

    private static final long serialVersionUID = 2107204559L;

    public static final QAccountEntity accountEntity = new QAccountEntity("accountEntity");

    public final QBaseTimeEntity _super = new QBaseTimeEntity(this);

    public final ComparablePath<java.util.UUID> accountId = createComparable("accountId", java.util.UUID.class);

    public final StringPath accountName = createString("accountName");

    public final StringPath accountNumber = createString("accountNumber");

    public final EnumPath<com.transfer.system.enums.AccountStatus> accountStatus = createEnum("accountStatus", com.transfer.system.enums.AccountStatus.class);

    public final EnumPath<com.transfer.system.enums.AccountType> accountType = createEnum("accountType", com.transfer.system.enums.AccountType.class);

    public final NumberPath<java.math.BigDecimal> balance = createNumber("balance", java.math.BigDecimal.class);

    public final StringPath bankName = createString("bankName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdTimeStamp = _super.createdTimeStamp;

    public final EnumPath<com.transfer.system.enums.CurrencyType> currencyType = createEnum("currencyType", com.transfer.system.enums.CurrencyType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedTimeStamp = _super.updatedTimeStamp;

    public QAccountEntity(String variable) {
        super(AccountEntity.class, forVariable(variable));
    }

    public QAccountEntity(Path<? extends AccountEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccountEntity(PathMetadata metadata) {
        super(AccountEntity.class, metadata);
    }

}

