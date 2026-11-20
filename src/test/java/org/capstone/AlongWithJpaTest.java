package org.capstone;


import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.capstone.domain.Cart;
import org.capstone.domain.CartItem;
import org.capstone.domain.Product;
import org.capstone.domain.SellerProfile;
import org.capstone.domain.User;
import org.capstone.domain.enums.ProductStatus;
import org.capstone.domain.enums.Role;
import org.capstone.repository.jpa.CartItemRepository;
import org.capstone.repository.jpa.CartRepository;
import org.capstone.repository.jpa.ProductRepository;
import org.capstone.repository.jpa.SellerProfileRepository;
import org.capstone.repository.jpa.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@DataJpaTest
public class AlongWithJpaTest {

    @Autowired
    EntityManager entityManager;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    SellerProfileRepository sellerProfileRepository;
    @Autowired
    PlatformTransactionManager transactionManager;

    private User testUser;

    @BeforeEach
    public void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@gmail.com")
                .passwordHash("password")
                .role(Role.BUYER)
                .build());
    }

    @Nested
    class ParentWithoutIdTest {
        @Test
        void saveCartWithoutIdUsing_JpaRepository() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            Cart repositoryCart = cartRepository.save(cart);
            log.info("Cart with id '{}' has been saved", repositoryCart.getId());
        }

        @Test
        void saveCartWithoutIdUsing_EntityManager_Persist() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            entityManager.persist(cart);

            log.info("Cart with id '{}' has been saved", cart.getId());
        }

        @Test
        void saveCartWithoutIdUsing_EntityManager_Merge() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            Cart mergeCart = entityManager.merge(cart);
            log.info("Cart with id '{}' has been saved", mergeCart.getId());
        }
    }

    @Nested
    class ParentWithIdTest {
        @Test
        void saveCartWithIdUsing_JpaRepository() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            Cart cartWithID = cartRepository.save(cart);

            cartRepository.save(cartWithID);
        }

        @Test
        void saveCartWithIdUsing_EntityManager_Persist() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            Cart cartWithID = cartRepository.save(cart);

            entityManager.persist(cartWithID);
        }

        @Test
        void saveCartWithIdUsing_EntityManager_Merge() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();

            Cart cartWithID = cartRepository.save(cart);

            entityManager.merge(cartWithID);
        }
    }

    @Nested
    class ParentWithSomeIDTest {
        @Test
        void saveCartWithSomeIdAndInsertAnotherCartWithSameIdUsing_JpaRepository() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();
            Cart cartWithID = cartRepository.save(cart);
            Cart cartWithSameID = Cart.builder()
                    .id(cartWithID.getId())
                    .user(testUser)
                    .build();

            cartRepository.save(cartWithSameID);
        }

        @Test
        void saveCartWithSomeIdAndInsertAnotherCartWithSameIdUsing_EntityManager_Persist() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();
            Cart cartWithID = cartRepository.save(cart);

            Cart cartWithSameID = Cart.builder()
                    .id(cartWithID.getId())
                    .user(testUser)
                    .build();

            assertThrows(EntityExistsException.class, () -> entityManager.persist(cartWithSameID));
        }

        @Test
        void saveCartWithSomeIdAndInsertAnotherCartWithSameIdUsing_EntityManager_Merge() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();
            Cart cartWithID = cartRepository.save(cart);
            Cart cartWithSameID = Cart.builder()
                    .id(cartWithID.getId())
                    .user(testUser)
                    .build();

            entityManager.merge(cartWithSameID);
        }
    }

    @Nested
    class ParentSaveWithoutChildrenTest {
        @Test
        void saveCartWithoutCartItemUsing_JpaRepository() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(CartItem.builder().build()))
                    .build();
            assertThrows(DataIntegrityViolationException.class, () -> cartRepository.save(cart));
        }

        @Test
        void saveCartWithoutCartItemUsing_EntityManager_Persist() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(CartItem.builder().build()))
                    .build();

            assertThrows(ConstraintViolationException.class, () -> entityManager.persist(cart));
        }

        @Test
        void saveCartWithoutCartItemUsing_EntityManager_Merge() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(CartItem.builder().build()))
                    .build();

            assertThrows(ConstraintViolationException.class, () -> entityManager.merge(cart));
        }
    }

    @Nested
    class ParentSaveWithChildrenTest {
        @Test
        void saveCartWithCartItemUsing_JpaRepository() {
            CartItem cartItem = createExistingCartItem();

            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(cartItem))
                    .build();
            cartItem.setCart(cart);

            Cart savedCart = cartRepository.save(cart);
            assertPersistedCartContainsItem(savedCart.getId(), cartItem.getId());
        }

        @Test
        void saveCartWithCartItemUsing_EntityManager_Persist() {
            CartItem cartItem = createExistingCartItem();

            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(cartItem))
                    .build();
            cartItem.setCart(cart);

            entityManager.persist(cart);
            assertPersistedCartContainsItem(cart.getId(), cartItem.getId());
        }

        @Test
        void saveCartWithCartItemUsing_EntityManager_Merge() {
            CartItem cartItem = createExistingCartItem();

            Cart cart = Cart.builder()
                    .user(testUser)
                    .cartItems(List.of(cartItem))
                    .build();
            cartItem.setCart(cart);

            Cart mergedCart = entityManager.merge(cart);
            assertPersistedCartContainsItem(mergedCart.getId(), cartItem.getId());
        }

        private CartItem createExistingCartItem() {
            User sourceCartUser = userRepository.save(User.builder()
                    .email("source-cart@gmail.com")
                    .passwordHash("password")
                    .role(Role.BUYER)
                    .build());
            Cart sourceCart = cartRepository.save(Cart.builder()
                    .user(sourceCartUser)
                    .build());

            CartItem cartItem = cartItemRepository.save(CartItem.builder()
                    .product(createExistingProduct())
                    .cart(sourceCart)
                    .build());
            entityManager.flush();
            return cartItem;
        }

        private void assertPersistedCartContainsItem(Long cartId, Long cartItemId) {
            entityManager.flush();
            entityManager.clear();

            Cart persistedCart = cartRepository.findById(cartId).orElseThrow();
            assertEquals(
                    List.of(cartItemId),
                    persistedCart.getCartItems().stream().map(CartItem::getId).toList());
        }
    }

    @Nested
    class ChildWithoutParentTest {
        @Test
        void saveCartItemWithoutCartUsing_JpaRepository() {
            CartItem cartItem = CartItem.builder()
                    .product(createExistingProduct())
                    .build();

            assertThrows(DataIntegrityViolationException.class, () -> cartItemRepository.save(cartItem));
        }

        @Test
        void saveCartItemWithoutCartUsing_EntityManager_Persist() {
            CartItem cartItem = CartItem.builder()
                    .product(createExistingProduct())
                    .build();

            assertThrows(ConstraintViolationException.class, () -> entityManager.persist(cartItem));
        }

        @Test
        void saveCartItemWithoutCartUsing_EntityManager_Merge() {
            CartItem cartItem = CartItem.builder()
                    .product(createExistingProduct())
                    .build();

            assertThrows(ConstraintViolationException.class, () -> entityManager.merge(cartItem));
        }
    }

    @Nested
    class ChildWithTransientParentTest {
        @Test
        void saveCartItemWithTransientCartUsing_JpaRepository() {
            CartItem cartItem = createCartItemWithTransientCart();

            assertThrows(InvalidDataAccessApiUsageException.class, () -> cartItemRepository.save(cartItem));
        }

        @Test
        void saveCartItemWithTransientCartUsing_EntityManager_Persist() {
            CartItem cartItem = createCartItemWithTransientCart();

            assertThrows(IllegalStateException.class, () -> entityManager.persist(cartItem));
        }

        @Test
        void saveCartItemWithTransientCartUsing_EntityManager_Merge() {
            CartItem cartItem = createCartItemWithTransientCart();

            assertThrows(IllegalStateException.class, () -> entityManager.merge(cartItem));
        }

        private CartItem createCartItemWithTransientCart() {
            Cart cart = Cart.builder()
                    .user(testUser)
                    .build();
            return CartItem.builder()
                    .product(createExistingProduct())
                    .cart(cart)
                    .build();
        }
    }

    @Nested
    class ChildWithDetachedParentTest {
        @Test
        void saveCartItemWithDetachedCartUsing_JpaRepository() {
            Cart cart = createDetachedCart();
            CartItem cartItem = cartItemRepository.save(createCartItem(cart));

            assertSavedCartItemHasParent(cartItem.getId(), cart.getId());
        }

        @Test
        void saveCartItemWithDetachedCartUsing_EntityManager_Persist() {
            Cart cart = createDetachedCart();
            CartItem cartItem = createCartItem(cart);

            entityManager.persist(cartItem);
            assertSavedCartItemHasParent(cartItem.getId(), cart.getId());
        }

        @Test
        void saveCartItemWithDetachedCartUsing_EntityManager_Merge() {
            Cart cart = createDetachedCart();
            CartItem cartItem = createCartItem(cart);

            CartItem mergedCartItem = entityManager.merge(cartItem);
            assertSavedCartItemHasParent(mergedCartItem.getId(), cart.getId());
        }

        private Cart createDetachedCart() {
            Cart cart = cartRepository.save(Cart.builder()
                    .user(testUser)
                    .build());
            entityManager.flush();
            entityManager.clear();

            Cart detachedCart = cartRepository.findById(cart.getId()).orElseThrow();
            entityManager.detach(detachedCart);
            return detachedCart;
        }

        private CartItem createCartItem(Cart cart) {
            return CartItem.builder()
                    .product(createExistingProduct())
                    .cart(cart)
                    .build();
        }

        private void assertSavedCartItemHasParent(Long cartItemId, Long cartId) {
            entityManager.flush();
            entityManager.clear();

            CartItem persistedCartItem = cartItemRepository.findById(cartItemId).orElseThrow();
            assertEquals(cartId, persistedCartItem.getCart().getId());
        }
    }

    @Nested
    class ParentDirtyCheckingTest {
        @Test
        void changesToManagedCartAreFlushedWithoutExplicitSave() {
            Cart cart = cartRepository.save(Cart.builder()
                    .user(testUser)
                    .build());

            Cart fetchedCart = cartRepository.findById(cart.getId()).orElseThrow();
            fetchedCart.setTotalAmount(new BigDecimal("12.34"));

            entityManager.flush();
            entityManager.clear();

            assertEquals(new BigDecimal("12.34"),
                    cartRepository.findById(cart.getId()).orElseThrow().getTotalAmount());
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void changesToManagedCartAreFlushedInExplicitTransactionWithoutSave() {
            Cart cart = cartRepository.save(Cart.builder()
                    .user(testUser)
                    .build());
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Cart fetchedCart = cartRepository.findById(cart.getId()).orElseThrow();
                    fetchedCart.setTotalAmount(new BigDecimal("56.78"));

                    entityManager.flush();
                    entityManager.clear();

                    assertEquals(new BigDecimal("56.78"),
                            cartRepository.findById(cart.getId()).orElseThrow().getTotalAmount());
                });
            } finally {
                cartRepository.deleteById(cart.getId());
                userRepository.deleteById(testUser.getId());
            }
        }
    }

    private Product createExistingProduct() {
        SellerProfile sellerProfile = sellerProfileRepository.save(SellerProfile.builder()
                .storeName("Test store")
                .user(testUser)
                .build());
        return productRepository.save(Product.builder()
                .title("Test product")
                .price(BigDecimal.ONE)
                .stockQuantity(10)
                .status(ProductStatus.ACTIVE)
                .sellerProfile(sellerProfile)
                .build());
    }

}
