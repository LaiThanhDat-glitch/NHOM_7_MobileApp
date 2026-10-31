package com.rescuefarm.data.local.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.rescuefarm.data.local.entity.OrderCacheEntity;
import com.rescuefarm.domain.enums.FulfillmentType;
import com.rescuefarm.domain.enums.OrderOwnerType;
import com.rescuefarm.domain.enums.OrderStatus;
import com.rescuefarm.domain.enums.PaymentMethod;
import com.rescuefarm.domain.enums.PaymentStatus;
import com.rescuefarm.domain.enums.ShipmentStatus;
import com.rescuefarm.domain.model.Order;
import com.rescuefarm.domain.model.OrderItem;
import com.rescuefarm.domain.model.Payment;
import com.rescuefarm.domain.model.Shipment;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderCacheMapperTest {

    @Test
    public void testItemsSerializationAndDeserialization() {
        List<OrderItem> items = new ArrayList<>();
        items.add(OrderItem.snapshot("item1", "order123", "prodA", "batchA",
                "Khoai lang mật", "https://img.rescuefarm.vn/khoai.jpg", "kg",
                25000.0, 15000.0, 15000.0, 3.5));
        items.add(OrderItem.snapshot("item2", "order123", "prodB", "batchB",
                "Thanh long ruột đỏ", "https://img.rescuefarm.vn/thanhlong.jpg", "kg",
                30000.0, 18000.0, 18000.0, 2.0));

        String serialized = OrderCacheMapper.serializeItems(items);
        assertNotNull(serialized);
        assertTrue(serialized.contains("Khoai lang mật"));
        assertTrue(serialized.contains("Thanh long ruột đỏ"));

        List<OrderItem> restored = OrderCacheMapper.deserializeItems(serialized);
        assertEquals(2, restored.size());

        OrderItem first = restored.get(0);
        assertEquals("item1", first.getId());
        assertEquals("order123", first.getOrderId());
        assertEquals("prodA", first.getProductId());
        assertEquals("batchA", first.getBatchId());
        assertEquals("Khoai lang mật", first.getProductNameSnapshot());
        assertEquals("https://img.rescuefarm.vn/khoai.jpg", first.getProductImageSnapshot());
        assertEquals("kg", first.getUnitSnapshot());
        assertEquals(25000.0, first.getOriginalPriceSnapshot(), 0.001);
        assertEquals(15000.0, first.getRescuePriceSnapshot(), 0.001);
        assertEquals(15000.0, first.getFinalPriceSnapshot(), 0.001);
        assertEquals(3.5, first.getQuantity(), 0.001);

        OrderItem second = restored.get(1);
        assertEquals("item2", second.getId());
        assertEquals("Thanh long ruột đỏ", second.getProductNameSnapshot());
        assertEquals(2.0, second.getQuantity(), 0.001);
    }

    @Test
    public void testPaymentSerializationAndDeserialization() {
        Payment payment = Payment.restore("pay123", "order123", PaymentMethod.BANK_TRANSFER,
                PaymentStatus.PAID, 125000.0, "REF888999", new Date(1700000000000L));
        payment.setBankDetails("MB", "NGUYEN VAN A", "0987654321");
        payment.setQrUrl("https://img.vietqr.io/image/MB-0987654321-compact.png");
        payment.setVerificationDetails("SEPAY", "SEVQR0123456789ABCDEF0123");

        String serialized = OrderCacheMapper.serializePayment(payment);
        assertNotNull(serialized);
        assertTrue(serialized.contains("SEPAY"));
        assertTrue(serialized.contains("SEVQR0123456789ABCDEF0123"));

        OrderCacheEntity entity = new OrderCacheEntity("order123");
        entity.paymentSerialized = serialized;
        entity.paymentMethod = PaymentMethod.BANK_TRANSFER.name();
        entity.paymentStatus = PaymentStatus.PAID.name();
        entity.totalAmount = 125000.0;

        Payment restored = OrderCacheMapper.toPaymentDomain(entity);
        assertNotNull(restored);
        assertEquals("pay123", restored.getId());
        assertEquals("order123", restored.getOrderId());
        assertEquals(PaymentMethod.BANK_TRANSFER, restored.getMethod());
        assertEquals(PaymentStatus.PAID, restored.getStatus());
        assertEquals(125000.0, restored.getAmount(), 0.001);
        assertEquals("REF888999", restored.getReferenceCode());
        assertEquals("MB", restored.getBankCode());
        assertEquals("NGUYEN VAN A", restored.getBankAccountName());
        assertEquals("0987654321", restored.getBankAccountNumber());
        assertEquals("SEPAY", restored.getVerificationProvider());
        assertTrue(restored.usesAutomaticVerification());
        assertEquals("SEVQR0123456789ABCDEF0123", restored.getTransferContent());
    }

    @Test
    public void testShipmentSerializationAndDeserialization() {
        Shipment shipment = Shipment.restore("ship123", "order123", "Giao Hàng Tiết Kiệm",
                "GHTK999888", ShipmentStatus.SHIPPING, 4.2, 22000.0);

        String serialized = OrderCacheMapper.serializeShipment(shipment);
        assertNotNull(serialized);
        assertTrue(serialized.contains("Giao Hàng Tiết Kiệm"));
        assertTrue(serialized.contains("GHTK999888"));

        OrderCacheEntity entity = new OrderCacheEntity("order123");
        entity.shipmentSerialized = serialized;
        entity.fulfillmentType = FulfillmentType.DELIVERY.name();
        entity.shippingFee = 22000.0;

        Shipment restored = OrderCacheMapper.toShipmentDomain(entity);
        assertNotNull(restored);
        assertEquals("ship123", restored.getId());
        assertEquals("order123", restored.getOrderId());
        assertEquals("Giao Hàng Tiết Kiệm", restored.getCarrierName());
        assertEquals("GHTK999888", restored.getTrackingCode());
        assertEquals(ShipmentStatus.SHIPPING, restored.getStatus());
        assertEquals(4.2, restored.getDistanceKm(), 0.001);
        assertEquals(22000.0, restored.getShippingFee(), 0.001);
    }

    @Test
    public void testFullOrderRoundtrip() {
        List<OrderItem> items = new ArrayList<>();
        items.add(OrderItem.snapshot("item1", "order999", "prod1", "batch1",
                "Dưa hấu Long An", "https://img.rescuefarm.vn/duahau.jpg", "quả",
                40000.0, 20000.0, 20000.0, 2.0));

        Date now = new Date();
        Order order = Order.restore("order999", OrderOwnerType.GUEST, "guest_uuid_123",
                "RF-20261031-999", "seller_farm_01", "camp_la_01",
                FulfillmentType.DELIVERY, "Trần Thị B", "0912345678", "123 Nguyễn Huệ, Q1, TP.HCM",
                10.7769, 106.7009, 40000.0, 0.0, 15000.0, 55000.0,
                PaymentMethod.COD, PaymentStatus.UNPAID, OrderStatus.CONFIRMED,
                "Giao buổi sáng", now, now, items)
                .withExpiryReview(false, "");

        Payment payment = Payment.create("order999", PaymentMethod.COD, 55000.0);
        Shipment shipment = Shipment.create("order999", 3.0, 15000.0);

        OrderCacheEntity entity = OrderCacheMapper.toEntity(order, payment, shipment, System.currentTimeMillis());
        assertNotNull(entity.itemsSerialized);
        assertNotNull(entity.paymentSerialized);
        assertNotNull(entity.shipmentSerialized);
        assertEquals("RF-20261031-999", entity.orderCode);
        assertEquals("0912345678", entity.receiverPhone);

        Order restoredOrder = OrderCacheMapper.toDomain(entity);
        assertEquals("order999", restoredOrder.getId());
        assertEquals("RF-20261031-999", restoredOrder.getOrderCode());
        assertEquals(OrderOwnerType.GUEST, restoredOrder.getOwnerType());
        assertEquals("guest_uuid_123", restoredOrder.getOwnerId());
        assertEquals("Trần Thị B", restoredOrder.getReceiverName());
        assertEquals("0912345678", restoredOrder.getReceiverPhone());
        assertEquals(55000.0, restoredOrder.getTotalAmount(), 0.001);
        assertEquals(1, restoredOrder.getItems().size());
        assertEquals("Dưa hấu Long An", restoredOrder.getItems().get(0).getProductNameSnapshot());

        Payment restoredPayment = OrderCacheMapper.toPaymentDomain(entity);
        assertNotNull(restoredPayment);
        assertEquals(PaymentMethod.COD, restoredPayment.getMethod());
        assertEquals(55000.0, restoredPayment.getAmount(), 0.001);

        Shipment restoredShipment = OrderCacheMapper.toShipmentDomain(entity);
        assertNotNull(restoredShipment);
        assertEquals(15000.0, restoredShipment.getShippingFee(), 0.001);
    }
}
