package com.rescuefarm.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;
import com.rescuefarm.data.local.dao.OrderCacheDao;
import com.rescuefarm.data.local.entity.OrderCacheEntity;
import com.rescuefarm.data.local.mapper.OrderCacheMapper;
import com.rescuefarm.domain.enums.FulfillmentType;
import com.rescuefarm.domain.enums.OrderOwnerType;
import com.rescuefarm.domain.enums.OrderStatus;
import com.rescuefarm.domain.enums.PaymentMethod;
import com.rescuefarm.domain.enums.PaymentStatus;
import com.rescuefarm.domain.model.Order;
import com.rescuefarm.domain.model.OrderItem;
import com.rescuefarm.domain.model.Payment;
import com.rescuefarm.domain.model.Shipment;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OfflineOrderRepositoryTest {

    private FakeOrderCacheDao fakeDao;
    private OfflineOrderRepository repository;
    private ExecutorService executor;

    @Before
    public void setUp() {
        fakeDao = new FakeOrderCacheDao();
        executor = Executors.newSingleThreadExecutor();
        repository = new OfflineOrderRepository(fakeDao, executor);
    }

    @Test
    public void testOfflineGuestOrderLookupSuccess() throws InterruptedException {
        List<OrderItem> items = new ArrayList<>();
        items.add(OrderItem.snapshot("item1", "order001", "p1", "b1",
                "Gạo ST25 giải cứu", "https://img.rescuefarm.vn/gao.jpg", "bao 5kg",
                180000.0, 120000.0, 120000.0, 1.0));

        Date now = new Date();
        Order order = Order.restore("order001", OrderOwnerType.GUEST, "guest_abc",
                "RF-20261031-001", "seller_01", "camp_01",
                FulfillmentType.PICKUP, "Nguyễn Văn C", "0909123456", "Điểm giải cứu Q1",
                10.77, 106.69, 120000.0, 0.0, 0.0, 120000.0,
                PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.PENDING,
                "Ghé lấy buổi chiều", now, now, items);

        OrderCacheEntity entity = OrderCacheMapper.toEntity(order, System.currentTimeMillis());
        fakeDao.replaceOrder(entity);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Order> resultOrder = new AtomicReference<>();
        AtomicReference<String> errorMsg = new AtomicReference<>();

        repository.findGuestOrder("RF-20261031-001", "0909123456", new OrderRepository.OrderCallback() {
            @Override
            public void onSuccess(Order o, boolean idempotentReplay) {
                resultOrder.set(o);
                latch.countDown();
            }

            @Override
            public void onError(OrderRepository.ErrorCode error, String message) {
                errorMsg.set(message);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultOrder.get());
        assertEquals("RF-20261031-001", resultOrder.get().getOrderCode());
        assertEquals("0909123456", resultOrder.get().getReceiverPhone());
        assertEquals(1, resultOrder.get().getItems().size());
        assertEquals("Gạo ST25 giải cứu", resultOrder.get().getItems().get(0).getProductNameSnapshot());
    }

    @Test
    public void testOfflineGetOrderDetailsWithPaymentAndShipment() throws InterruptedException {
        Order order = Order.restore("order002", OrderOwnerType.GUEST, "guest_xyz",
                "RF-20261031-002", "seller_02", "camp_02",
                FulfillmentType.DELIVERY, "Lê Văn D", "0988777666", "456 Lê Lợi, Q1",
                10.77, 106.70, 200000.0, 0.0, 20000.0, 220000.0,
                PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, OrderStatus.CONFIRMED,
                "", new Date(), new Date(), Collections.emptyList());

        Payment payment = Payment.restore("pay002", "order002", PaymentMethod.BANK_TRANSFER,
                PaymentStatus.PAID, 220000.0, "MB_TXN_001", new Date());
        payment.setBankDetails("MB", "CHỦ NÔNG TRẠI", "1122334455");

        Shipment shipment = Shipment.restore("ship002", "order002", "Ahamove",
                "AHA12345", com.rescuefarm.domain.enums.ShipmentStatus.SHIPPING, 2.5, 20000.0);

        OrderCacheEntity entity = OrderCacheMapper.toEntity(order, payment, shipment, System.currentTimeMillis());
        fakeDao.replaceOrder(entity);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Order> resultOrder = new AtomicReference<>();
        AtomicReference<Payment> resultPayment = new AtomicReference<>();
        AtomicReference<Shipment> resultShipment = new AtomicReference<>();

        repository.getOrder("order002", new OrderRepository.LifecycleCallback() {
            @Override
            public void onSuccess(Order o, Payment p, Shipment s, boolean idempotentReplay) {
                resultOrder.set(o);
                resultPayment.set(p);
                resultShipment.set(s);
                latch.countDown();
            }

            @Override
            public void onError(OrderRepository.ErrorCode error, String message) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultOrder.get());
        assertEquals("RF-20261031-002", resultOrder.get().getOrderCode());

        assertNotNull(resultPayment.get());
        assertEquals(PaymentMethod.BANK_TRANSFER, resultPayment.get().getMethod());
        assertEquals("MB", resultPayment.get().getBankCode());

        assertNotNull(resultShipment.get());
        assertEquals("Ahamove", resultShipment.get().getCarrierName());
        assertEquals("AHA12345", resultShipment.get().getTrackingCode());
    }

    @Test
    public void testOfflineLookupNotFound() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        repository.findGuestOrder("NOT_EXIST", "0000000000", new OrderRepository.OrderCallback() {
            @Override
            public void onSuccess(Order order, boolean idempotentReplay) {
                latch.countDown();
            }

            @Override
            public void onError(OrderRepository.ErrorCode error, String message) {
                errorCalled.set(true);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorCalled.get());
    }

    private static class FakeOrderCacheDao implements OrderCacheDao {
        private final Map<String, OrderCacheEntity> storage = new HashMap<>();

        @Override
        public LiveData<List<OrderCacheEntity>> observeOwnerOrders(String ownerId) {
            return null;
        }

        @Override
        public LiveData<List<OrderCacheEntity>> observeSellerOrders(String sellerId) {
            return null;
        }

        @Override
        public OrderCacheEntity find(String orderId) {
            return storage.get(orderId);
        }

        @Override
        public OrderCacheEntity findByCodeAndPhone(String code, String phone) {
            for (OrderCacheEntity entity : storage.values()) {
                boolean codeMatches = (code != null && (code.equals(entity.orderCode) || code.equals(entity.id)));
                boolean phoneMatches = (phone == null || phone.isEmpty() || phone.equals(entity.receiverPhone));
                if (codeMatches && phoneMatches) return entity;
            }
            return null;
        }

        @Override
        public OrderCacheEntity findByCode(String code) {
            for (OrderCacheEntity entity : storage.values()) {
                if (code != null && (code.equals(entity.orderCode) || code.equals(entity.id))) {
                    return entity;
                }
            }
            return null;
        }

        @Override
        public List<OrderCacheEntity> getRecentGuestOrders(int limit) {
            return new ArrayList<>(storage.values());
        }

        @Override
        public LiveData<List<OrderCacheEntity>> observeGuestOrders() {
            return null;
        }

        @Override
        public void replaceOrders(List<OrderCacheEntity> orders) {
            for (OrderCacheEntity e : orders) replaceOrder(e);
        }

        @Override
        public void replaceOrder(OrderCacheEntity order) {
            storage.put(order.id, order);
        }

        @Override
        public void clearOwnerOrders(String ownerId) {
            storage.values().removeIf(e -> ownerId.equals(e.ownerId));
        }

        @Override
        public void clearSellerOrders(String sellerId) {
            storage.values().removeIf(e -> sellerId.equals(e.sellerId));
        }

        @Override
        public void deleteOlderThan(long cutoffEpochMillis) {
            storage.values().removeIf(e -> e.cachedAtEpochMillis < cutoffEpochMillis);
        }
    }
}
