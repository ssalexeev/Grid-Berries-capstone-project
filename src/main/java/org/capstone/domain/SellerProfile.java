package org.capstone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seller_profile")
public class SellerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sellerProfileSeqGen")
    @SequenceGenerator(name = "sellerProfileSeqGen", sequenceName = "seller_profile_id_seq")
    private Long id;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_description")
    private String storeDescription;

    @Column(name = "contact_info")
    private String contactInfo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
