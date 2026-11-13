package org.capstone.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerProfileResponse {
    private Long id;
    private String storeName;
    private String storeDescription;
    private String contactInfo;
    private LocalDateTime createdAt;
}
