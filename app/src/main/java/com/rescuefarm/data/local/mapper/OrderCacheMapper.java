package com.rescuefarm.data.local.mapper;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class OrderCacheMapper {
    private OrderCacheMapper() { }

    public static OrderCacheEntity toEntity(Order value, long cachedAt) {
        return toEntity(value, null, null, cachedAt);
    }

    public static OrderCacheEntity toEntity(Order value, Payment payment, Shipment shipment, long cachedAt) {
        OrderCacheEntity entity = new OrderCacheEntity(value.getId());
        entity.ownerType = value.getOwnerType().name(); entity.ownerId = value.getOwnerId();
        entity.orderCode = value.getOrderCode(); entity.sellerId = value.getSellerId();
        entity.campaignId = value.getCampaignId(); entity.fulfillmentType = value.getFulfillmentType().name();
        entity.receiverName = value.getReceiverName(); entity.receiverPhone = value.getReceiverPhone();
        entity.receiverAddress = value.getReceiverAddress(); entity.receiverLatitude = value.getReceiverLatitude();
        entity.receiverLongitude = value.getReceiverLongitude(); entity.subtotal = value.getSubtotal();
        entity.quantityDiscount = value.getQuantityDiscount(); entity.shippingFee = value.getShippingFee();
        entity.totalAmount = value.getTotalAmount(); entity.paymentMethod = value.getPaymentMethod().name();
        entity.paymentStatus = value.getPaymentStatus().name(); entity.status = value.getStatus().name();
        entity.note = value.getNote(); entity.createdAtEpochMillis = millis(value.getCreatedAt());
        entity.expiryReviewRequired = value.isExpiryReviewRequired(); entity.cancellationReason = value.getCancellationReason();
        entity.updatedAtEpochMillis = millis(value.getUpdatedAt()); entity.cachedAtEpochMillis = cachedAt;

        if (value.getItems() != null && !value.getItems().isEmpty()) {
            entity.itemsSerialized = serializeItems(value.getItems());
        }
        if (payment != null) {
            entity.paymentSerialized = serializePayment(payment);
        }
        if (shipment != null) {
            entity.shipmentSerialized = serializeShipment(shipment);
        }
        return entity;
    }

    public static Order toDomain(OrderCacheEntity entity) {
        List<OrderItem> items = deserializeItems(entity.itemsSerialized);
        return Order.restore(entity.id, value(OrderOwnerType.class, entity.ownerType, OrderOwnerType.GUEST),
                entity.ownerId, entity.orderCode, entity.sellerId, entity.campaignId,
                value(FulfillmentType.class, entity.fulfillmentType, FulfillmentType.PICKUP),
                entity.receiverName, entity.receiverPhone, entity.receiverAddress,
                entity.receiverLatitude, entity.receiverLongitude, entity.subtotal,
                entity.quantityDiscount, entity.shippingFee, entity.totalAmount,
                value(PaymentMethod.class, entity.paymentMethod, PaymentMethod.COD),
                value(PaymentStatus.class, entity.paymentStatus, PaymentStatus.UNPAID),
                value(OrderStatus.class, entity.status, OrderStatus.PENDING), entity.note,
                date(entity.createdAtEpochMillis), date(entity.updatedAtEpochMillis), items)
                .withExpiryReview(entity.expiryReviewRequired, entity.cancellationReason);
    }

    public static Payment toPaymentDomain(OrderCacheEntity entity) {
        if (entity.paymentSerialized != null && !entity.paymentSerialized.trim().isEmpty()) {
            try {
                JSONObject d = new JSONObject(entity.paymentSerialized);
                Payment p = Payment.restore(d.optString("id"), d.optString("orderId"),
                        value(PaymentMethod.class, d.optString("method"), PaymentMethod.COD),
                        value(PaymentStatus.class, d.optString("status"), PaymentStatus.UNPAID),
                        d.optDouble("amount"), d.optString("referenceCode"), date(d.optLong("paidAt")));
                p.setBankDetails(d.optString("bankCode"), d.optString("bankAccountName"), d.optString("bankAccountNumber"));
                p.setQrUrl(d.optString("qrUrl"));
                p.setVerificationDetails(d.optString("verificationProvider"), d.optString("transferContent"));
                return p;
            } catch (Exception ignored) { }
        }
        Payment p = Payment.restore(entity.id, entity.id,
                value(PaymentMethod.class, entity.paymentMethod, PaymentMethod.COD),
                value(PaymentStatus.class, entity.paymentStatus, PaymentStatus.UNPAID),
                entity.totalAmount, "", null);
        return p;
    }

    public static Shipment toShipmentDomain(OrderCacheEntity entity) {
        if (entity.shipmentSerialized != null && !entity.shipmentSerialized.trim().isEmpty()) {
            try {
                JSONObject d = new JSONObject(entity.shipmentSerialized);
                return Shipment.restore(d.optString("id"), d.optString("orderId"),
                        d.optString("carrierName"), d.optString("trackingCode"),
                        value(ShipmentStatus.class, d.optString("status"), ShipmentStatus.PENDING),
                        d.optDouble("distanceKm"), d.optDouble("shippingFee"));
            } catch (Exception ignored) { }
        }
        if (FulfillmentType.DELIVERY.name().equals(entity.fulfillmentType)) {
            return Shipment.create(entity.id, 0D, entity.shippingFee);
        }
        return null;
    }

    public static String serializeItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return "";
        try {
            JSONArray array = new JSONArray();
            for (OrderItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("orderId", item.getOrderId());
                obj.put("productId", item.getProductId());
                obj.put("batchId", item.getBatchId());
                obj.put("productNameSnapshot", item.getProductNameSnapshot());
                obj.put("productImageSnapshot", item.getProductImageSnapshot());
                obj.put("unitSnapshot", item.getUnitSnapshot());
                obj.put("originalPriceSnapshot", item.getOriginalPriceSnapshot());
                obj.put("rescuePriceSnapshot", item.getRescuePriceSnapshot());
                obj.put("finalPriceSnapshot", item.getFinalPriceSnapshot());
                obj.put("quantity", item.getQuantity());
                array.put(obj);
            }
            return array.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static List<OrderItem> deserializeItems(String raw) {
        List<OrderItem> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.optJSONObject(i);
                if (row == null) continue;
                result.add(OrderItem.snapshot(
                        row.optString("id"),
                        row.optString("orderId"),
                        row.optString("productId"),
                        row.optString("batchId"),
                        row.optString("productNameSnapshot"),
                        row.optString("productImageSnapshot"),
                        row.optString("unitSnapshot"),
                        row.optDouble("originalPriceSnapshot"),
                        row.optDouble("rescuePriceSnapshot"),
                        row.optDouble("finalPriceSnapshot"),
                        row.optDouble("quantity")
                ));
            }
        } catch (Exception ignored) { }
        return result;
    }

    public static String serializePayment(Payment payment) {
        if (payment == null) return "";
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", payment.getId());
            obj.put("orderId", payment.getOrderId());
            obj.put("method", payment.getMethod() == null ? "COD" : payment.getMethod().name());
            obj.put("status", payment.getStatus() == null ? "UNPAID" : payment.getStatus().name());
            obj.put("amount", payment.getAmount());
            obj.put("referenceCode", payment.getReferenceCode());
            if (payment.getPaidAt() != null) obj.put("paidAt", payment.getPaidAt().getTime());
            obj.put("bankCode", payment.getBankCode());
            obj.put("bankAccountName", payment.getBankAccountName());
            obj.put("bankAccountNumber", payment.getBankAccountNumber());
            obj.put("qrUrl", payment.getQrUrl());
            obj.put("verificationProvider", payment.getVerificationProvider());
            obj.put("transferContent", payment.getTransferContent());
            return obj.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String serializeShipment(Shipment shipment) {
        if (shipment == null) return "";
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", shipment.getId());
            obj.put("orderId", shipment.getOrderId());
            obj.put("carrierName", shipment.getCarrierName());
            obj.put("trackingCode", shipment.getTrackingCode());
            obj.put("status", shipment.getStatus() == null ? "PENDING" : shipment.getStatus().name());
            obj.put("distanceKm", shipment.getDistanceKm());
            obj.put("shippingFee", shipment.getShippingFee());
            return obj.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static List<Order> orders(List<OrderCacheEntity> entities) {
        List<Order> result = new ArrayList<>();
        if (entities != null) for (OrderCacheEntity entity : entities) {
            try { result.add(toDomain(entity)); } catch (RuntimeException ignored) { }
        }
        return result;
    }

    private static long millis(Date value) { return value == null ? 0L : value.getTime(); }
    private static Date date(long value) { return value <= 0L ? null : new Date(value); }
    private static <T extends Enum<T>> T value(Class<T> type, String raw, T fallback) {
        try { return Enum.valueOf(type, raw == null ? "" : raw); }
        catch (IllegalArgumentException error) { return fallback; }
    }
}
