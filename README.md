# 계좌 간 송금 시스템
## 프로젝트 개요
- 본 프로젝트는 Spring Boot와 JPA를 사용하여 계좌 간 송금 시스템의 핵심 기능을 구현한 백엔드 API 서버입니다.
- RESTful API를 통해 계좌 관리, 입출금, 이체, 거래 내역 조회 기능을 제공하며 Docker Compose를 사용하여 실행 환경을 구축할 수 있습니다.

<br>

## 기술 스택
- Backend
  - Java 21
  - Spring Boot 3.5.4
- Database
  - JPA (Hibernate)
  - H2 Database (테스트용)
  - Flyway (데이터베이스 마이그레이션)
- Build Tool
  - Gradle 8.14.3
- Infrastructure
  - Docker, Docker Compose
- API Documentation
  - Swagger

<br>

## 주요 기능

- 계좌 관리
  - 신규 계좌 생성 및 기존 계좌 삭제
- 입출금
  - 특정 계좌에 대한 입금, 출금 처리
  - 출금 시 일일 한도 적용 (1,000,000원)
- 계좌 이체
  - 다른 계좌로 금액 이체
  - 이체 시 일일 한도 적용 (3,000,000원)
  - 수수료 정책 : 이체 금액의 1%
- 거래 내역 조회
  - 특정 계좌의 거래 내역을 최신 순으로 페이징 조회
- Swagger 이용한 API 명세 자동화
- 단위 테스트 및 통합 테스트

<br>

## 실행 방법

1. Docker Desktop 실행

2. 프로젝트 클론 및 빌드
   
    ```bash
    git clone {github_url}
    cd {directory_name}
    ```
   
3. 다음 명령어로 컨테이너 실행

    ```bash
   ./gradlew clean :module-api:bootJar
    docker compose up --build
    ```

4. 서버 실행 확인 및 Swagger UI 접속

    - http://localhost:8080/swagger-ui/index.html

<br>

## API 문서

### 계좌 생성

* **Endpoint**
    - `/api/account/create`
* **Method**
    - `POST`
* **Description**
    - 새로운 계좌를 생성함
* **Request Body**

  ```json
  {
    "accountName": "Test Account",
    "accountType": "PERSONAL",
    "currencyType": "KRW"
  }
  ```
  ```json
  {
    "result_code": 1,
    "data": {
        "accountId": "7abc0907-2df8-421d-b54a-f440c9bbb056",
        "accountNumber": "00125081300001",
        "accountName": "Test Account",
        "bankName": "mxxikrBank",
        "accountType": "PERSONAL",
        "currencyType": "KRW",
        "balance": 0,
        "accountStatus": "ACTIVE",
        "createdTimeStamp": "2025-08-13T14:41:42.750121863",
        "updatedTimeStamp": "2025-08-13T14:41:42.750138194"
    },
    "message": "계좌 생성이 완료되었습니다.",
    "timestamp": "2025-08-13T14:41:42.769402851"
  }
  ```

### 계좌 조회

* **Endpoint**
    - `/api/account/{accountId}`
* **Method**
    - `GET`
* **Description**
    - 계좌의 상세 정보를 조회함
* **Path Variable**
    - `accountId` (UUID)
* **Response Body**

  ```json
  {
    "result_code": 1,
    "data": {
        "accountId": "7abc0907-2df8-421d-b54a-f440c9bbb056",
        "accountNumber": "00125081300001",
        "accountName": "Test Account",
        "bankName": "mxxikrBank",
        "accountType": "PERSONAL",
        "currencyType": "KRW",
        "balance": 0.00,
        "accountStatus": "ACTIVE",
        "createdTimeStamp": "2025-08-13T14:41:42.750122",
        "updatedTimeStamp": "2025-08-13T14:41:42.750138"
    },
    "message": "계좌 조회가 완료되었습니다.",
    "timestamp": "2025-08-13T14:45:30.398245756"
  }
  ```

### 계좌 삭제

* **Endpoint**
    - `/api/account/{accountId}`
* **Method**
    - `DELETE`
* **Description**
    - 계좌를 삭제 처리함
* **Response Body**

  ```json
  {
    "data": null,
    "message": "계좌 삭제가 완료되었습니다.",
    "timestamp": "2025-08-14T17:22:20.675108143",
    "result_code": 0
  }
  ```

### 입금 처리

* **Endpoint**
    - `/api/account/deposit`
* **Method**
    - `POST`
* **Description**
    - 특정 계좌에 입금 처리함
* **Request Body**

  ```json
  {
    "accountNumber": "00125081300001",
    "amount": 100000
  }
  ```
* **Response Body**

  ```json
  {
    "result_code": 1,
    "data": {
        "accountNumber": "00125081300001",
        "amount": 10000.00,
        "balance": 40000.00
    },
    "message": "입금이 완료되었습니다.",
    "timestamp": "2025-08-13T15:39:16.547038319"
  }
  ```

### 출금 처리

* **Endpoint**
    - `/api/account/withdraw`
* **Method**
    - `POST`
* **Description**
    - 특정 계좌에서 출금 처리함
    - 일일 출금 한도 : 1,000,000원
* **Request Body**

  ```json
  {
    "accountNumber": "00125081400001",
    "amount": 100000
  }
  ```
* **Response Body**

  ```json
  {
    "data": {
        "accountNumber": "00125081400001",
        "amount": 10000.00,
        "balance": 10000.00
    },
    "message": "출금이 완료되었습니다.",
    "timestamp": "2025-08-14T12:10:19.207306584",
    "result_code": 1
  }
  ```

### 계좌 이체

* **Endpoint**
    - `/api/transaction/transfer`
* **Method**
    - `POST`
* **Description**
    - 송신 계좌에서 수신 계좌로 이체를 진행함
    - 수수료 : 이체 금액의 1%
* **Request Body**

  ```json
  {
    "fromAccountNumber": "00125081300002",
    "toAccountNumber": "00125081300003",
    "amount": 100
  }
  ```
* **Response Body**

  ```json
  {
    "result_code": 1,
    "data": {
        "transactionId": "9232e60a-2e11-466e-be8d-e237918a551f",
        "fromAccountNumber": "00125081300002",
        "toAccountNumber": "00125081300003",
        "transactionType": "TRANSFER",
        "amount": 100,
        "fee": 1.00,
        "createdTimeStamp": "2025-08-13T14:53:49.008178389"
    },
    "message": "이체가 완료되었습니다.",
    "timestamp": "2025-08-13T14:53:49.01116192"
  }
  ```

### 거래 내역 조회

* **Endpoint**
    - `/api/transaction/history/`
* **Method**
    - `GET`
* **Description**
    - 특정 계좌의 거래 내역을 최신 순으로 페이징 조회함
* **Query Parameters**

    * `accountNumber`: 조회할 계좌 번호
    * `page`: 페이지 번호(0부터 시작)
    * `size`: 한 페이지에 보여줄 개수
* **Response Body**

  ```json
  {
    "result_code": 1,
    "data": {
        "content": [
            {
                "transactionId": "9232e60a-2e11-466e-be8d-e237918a551f",
                "fromAccountNumber": "00125081300002",
                "toAccountNumber": "00125081300003",
                "transactionType": "TRANSFER",
                "amount": 100.00,
                "fee": 1.00,
                "createdTimeStamp": "2025-08-13T14:53:49.008178"
            },
            {
                "transactionId": "2cf08c1a-bb67-42d5-8729-063af05a1585",
                "fromAccountNumber": "00125081300002",
                "toAccountNumber": "00125081300003",
                "transactionType": "TRANSFER",
                "amount": 1.00,
                "fee": 0.01,
                "createdTimeStamp": "2025-08-13T14:53:42.627225"
            },
            {
                "transactionId": "24fbb88f-59e9-4559-a9e5-e91fc1e93c6c",
                "toAccountNumber": "00125081300002",
                "transactionType": "DEPOSIT",
                "amount": 10000.00,
                "fee": 0.00,
                "createdTimeStamp": "2025-08-13T14:53:30.059751"
            },
            {
                "transactionId": "d9c7add9-b115-4608-894c-a01a83fb3a77",
                "toAccountNumber": "00125081300002",
                "transactionType": "DEPOSIT",
                "amount": 10000.00,
                "fee": 0.00,
                "createdTimeStamp": "2025-08-13T14:53:29.399545"
            },
            {
                "transactionId": "a853d9c7-51a7-4162-8994-ba6bd0fe9562",
                "toAccountNumber": "00125081300002",
                "transactionType": "DEPOSIT",
                "amount": 10000.00,
                "fee": 0.00,
                "createdTimeStamp": "2025-08-13T14:53:28.742858"
            },
            {
                "transactionId": "0f977d23-1192-47c2-b1e5-ea55e97d8b5c",
                "fromAccountNumber": "00125081300002",
                "transactionType": "WITHDRAW",
                "amount": 10000.00,
                "fee": 0.00,
                "createdTimeStamp": "2025-08-13T14:51:20.361416"
            },
            {
                "transactionId": "e7aa77fc-f456-4009-9e4b-ddaf715fb867",
                "toAccountNumber": "00125081300002",
                "transactionType": "DEPOSIT",
                "amount": 10000.00,
                "fee": 0.00,
                "createdTimeStamp": "2025-08-13T14:49:29.473054"
            }
        ],
        "pageable": {
            "pageNumber": 0,
            "pageSize": 10,
            "sort": {
                "sorted": true,
                "empty": false,
                "unsorted": false
            },
            "offset": 0,
            "paged": true,
            "unpaged": false
        },
        "last": true,
        "totalElements": 7,
        "totalPages": 1,
        "size": 10,
        "number": 0,
        "sort": {
            "sorted": true,
            "empty": false,
            "unsorted": false
        },
        "first": true,
        "numberOfElements": 7,
        "empty": false
    },
    "message": "거래 내역 조회가 완료되었습니다.",
    "timestamp": "2025-08-13T15:00:42.729606369"
  }
  ```

<br>

## 상태 및 오류 코드

### 결과 코드(`result_code`)
  - `1`
    - 성공
    - 요청이 성공적으로 처리되었으며 data 필드에 결과가 포함 경우 반환
  - `0`
    - 데이터 없음
    - 요청은 성공했으나 반환할 데이터가 없는 경우 반환
  - `-1`
    - 데이터 조회 실패/오류
    - 데이터 처리 중 오류가 발생했을 경우 반환
  - `-2`
    - 입력 파라미터 오류
    - 요청으로 전달된 파라미터가 유효하지 않을 경우 반환
  - `-3`
    - 서버 오류
    - 내부 서버 오류가 발생할 경우 반환

### 오류 코드

- **계좌(Account) 관련 오류**
  - `404 NOT_FOUND`
    - **message**: `계좌를 찾을 수 없습니다.`
    - **description**: 요청한 계좌번호에 해당하는 계좌가 존재하지 않을 경우 반환
  - `409 CONFLICT`
    - **message**: `이미 존재하는 계좌 번호입니다.`
    - **description**: 계좌 생성 시 이미 사용 중인 계좌번호일 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `같은 계좌로 이체할 수 없습니다.`
    - **description**: 출금 계좌와 입금 계좌가 동일할 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `출금 한도를 초과했습니다.`
    - **description**: 1일 출금 한도를 초과하여 출금을 시도할 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `해당 계좌에 거래 이력이 있어 삭제할 수 없습니다.`
    - **description**: 거래 내역이 있는 활성 계좌를 삭제하려고 할 경우 반환

- **거래(Transaction) 관련 오류**
  - `400 BAD_REQUEST`
    - **message**: `잔액이 부족합니다.`
    - **description**: 출금 또는 이체 시 계좌의 잔액이 요청 금액보다 부족할 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `이체 한도를 초과했습니다.`
    - **description**: 1일 이체 한도를 초과하여 이체를 시도할 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `수신 계좌가 비활성화 상태입니다.`
      - **description**: 입금받는 쪽의 계좌가 비활성(삭제) 상태일 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `송신 계좌가 비활성화 상태입니다.`
    - **description**: 돈을 보내는 쪽의 계좌가 비활성(삭제) 상태일 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `유효하지 않은 금액입니다.`
    - **description**: 금액이 0원 이하인 등 유효하지 않은 값일 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `통화 종류가 일치하지 않습니다.`
    - **description**: 두 계좌의 통화(Currency) 타입이 다를 경우 반환
  - `400 BAD_REQUEST`
    - **message**: `유효하지 않은 요청입니다.`
    - **description**: 기타 유효성 검사에 실패한 일반적인 요청 오류일 경우 반환

- **서버 오류**
  - `500 INTERNAL_SERVER_ERROR`
    - **message**: `서버 오류가 발생했습니다.`
    - **description**: 예측하지 못한 내부 서버 오류가 발생했을 경우 반환

- **Response Body**
  ```json
  {
    "result_code": 0,
    "data": null,
    "message": "계좌 삭제가 완료되었습니다.",
    "timestamp": "2025-08-13T15:41:37.268933927"
  }
  ```
  ```json
  {
    "result_code": -1,
    "data": null,
    "message": "잔액이 부족합니다.",
    "timestamp": "2025-08-13T15:40:11.819357876"
  }
  ```
  ```json
  {
    "result_code": -2,
    "data": null,
    "message": "유효하지 않은 요청입니다.",
    "timestamp": "2025-08-13T16:01:24.190524344"
  }
  ```
  ```json
  {
    "result_code": -3,
    "data": null,
    "message": "서버 오류가 발생했습니다.",
    "timestamp": "2025-08-13T14:39:39.767059197"
  }
  ```

<br>

## 시스템 정책 및 제약 사항

* 출금 일일 한도 : 1,000,000원
* 이체 일일 한도 : 3,000,000원
* 이체 시 수수료 : 1%
  * 소수점 둘째 자리까지 반올림 (HALF_UP) 처리
* 거래 불가 조건
  * 송금 또는 수신 계좌 상태가 `INACTIVE`일 경우 이체 불가
  * 동일 계좌 간 이체 금지
* 계좌 삭제 불가 조건
  * 거래 내역이 있고 계좌 상태가 ACTIVE인 경우 삭제 불가
* 페이징 요청
  * 기본 페이지 번호: 0
  * 기본 페이지 크기: 10
  * 최대 페이지 크기: 100
  * 거래 정렬 필드: `createdTimeStamp`