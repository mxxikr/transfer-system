package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;

import java.util.UUID;

public interface AccountService {

    AccountEntity createAccount(String accountName, AccountType accountType, CurrencyType currencyType);

    AccountEntity getAccount(UUID id);

    void deleteAccount(UUID id);
}