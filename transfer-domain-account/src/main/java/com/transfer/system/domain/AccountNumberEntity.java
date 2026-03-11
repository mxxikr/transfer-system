package com.transfer.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "account_number_sequence")
@NoArgsConstructor
@AllArgsConstructor
public class AccountNumberEntity {
    @Id
    private LocalDate id;  // 날짜

    @Column(nullable = false)
    private Long lastNumber; // 마지막 사용 번호
}