-- 날짜별 계좌 번호 시퀀스 테이블
CREATE TABLE account_number_sequence (
     id DATE PRIMARY KEY, -- 날짜별 관리
     last_number BIGINT NOT NULL -- 마지막 사용 시퀀스 번호
);

-- 계좌 정보 테이블
CREATE TABLE account_entity (
    account_id BINARY(16) NOT NULL PRIMARY KEY, -- 계좌 고유 식별자
    account_number VARCHAR(14) NOT NULL UNIQUE, -- 계좌 번호
    account_name VARCHAR(255), -- 계좌 이름
    bank_name VARCHAR(255), -- 은행 이름
    account_type VARCHAR(255), -- 계좌 유형
    currency_type VARCHAR(255), -- 통화 유형
    balance DECIMAL(19, 2) NOT NULL, -- 계좌 잔액
    account_status VARCHAR(255), -- 계좌 상태
    created_time_stamp TIMESTAMP, -- 계좌 생성 시간
    updated_time_stamp TIMESTAMP -- 계좌 정보 수정 시간
);

CREATE UNIQUE INDEX ux_account_entity_account_number ON account_entity (account_number);

-- 거래 정보 테이블
CREATE TABLE transaction_entity (
    transaction_id BINARY(16) NOT NULL PRIMARY KEY, -- 거래 고유 식별자
    from_account_id BINARY(16), -- 출금 계좌 고유 식별자
    to_account_id BINARY(16), -- 입금 계좌 고유 식별자
    transaction_type VARCHAR(255) NOT NULL, -- 거래 유형
    amount DECIMAL(19, 2) NOT NULL, -- 거래 금액
    fee DECIMAL(19, 2), -- 거래 수수료
    created_time_stamp TIMESTAMP, -- 거래 생성 시간
    CONSTRAINT fk_from_account FOREIGN KEY (from_account_id) REFERENCES account_entity(account_id),
    CONSTRAINT fk_to_account FOREIGN KEY (to_account_id) REFERENCES account_entity(account_id)
);

CREATE INDEX idx_tx_from_created ON transaction_entity (from_account_id, created_time_stamp);
CREATE INDEX idx_tx_to_created ON transaction_entity (to_account_id, created_time_stamp);
CREATE INDEX idx_tx_from_type_created ON transaction_entity (from_account_id, transaction_type, created_time_stamp);