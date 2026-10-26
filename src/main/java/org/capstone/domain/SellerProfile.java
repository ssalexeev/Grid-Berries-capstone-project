package org.capstone.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {
    private Long id;

    private String storeName;

    private String storeDescription;

    private String contactInfo;

    private User user;

    private LocalDateTime createdAt;
}
