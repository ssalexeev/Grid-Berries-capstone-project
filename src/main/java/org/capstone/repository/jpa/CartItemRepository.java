package org.capstone.repository.jpa;

import org.capstone.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByCart_Id(Long cartId);

    void deleteAllByCart_Id(Long cartId);
}
