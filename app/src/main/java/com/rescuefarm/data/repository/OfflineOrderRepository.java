package com.rescuefarm.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.rescuefarm.data.local.dao.OrderCacheDao;
import com.rescuefarm.data.local.entity.OrderCacheEntity;
import com.rescuefarm.data.local.mapper.OrderCacheMapper;
import com.rescuefarm.domain.enums.OrderStatus;
import com.rescuefarm.domain.model.Order;
import com.rescuefarm.service.order.CheckoutRequest;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class OfflineOrderRepository implements OrderRepository {
    private static final String READ_MESSAGE = "Đang offline; danh sách đơn tiếp tục dùng Room cache.";
    private static final String WRITE_MESSAGE = "Thao tác đơn hàng quan trọng cần kết nối mạng và không được xếp hàng offline.";
    private final OrderCacheDao dao; private final ExecutorService executor;
    public OfflineOrderRepository(OrderCacheDao dao, ExecutorService executor) { this.dao=dao;this.executor=executor; }
    @Override public LiveData<List<Order>> observeOrders(String ownerId){return Transformations.map(
            dao.observeOwnerOrders(ownerId),OrderCacheMapper::orders);}
    @Override public LiveData<List<Order>> observeSellerOrders(String sellerId){return Transformations.map(
            dao.observeSellerOrders(sellerId),OrderCacheMapper::orders);}
    @Override public void checkout(CheckoutRequest request,OrderCallback callback){callback.onError(ErrorCode.NETWORK,WRITE_MESSAGE);}
    @Override public void refreshOrders(String ownerId,ActionCallback callback){callback.onError(ErrorCode.NETWORK,READ_MESSAGE);}
    @Override public void refreshSellerOrders(String sellerId,ActionCallback callback){callback.onError(ErrorCode.NETWORK,READ_MESSAGE);}
    @Override public void loadMoreSellerOrders(String sellerId,ActionCallback callback){callback.onError(ErrorCode.NETWORK,READ_MESSAGE);}
    @Override public void findGuestOrder(String code,String phone,OrderCallback callback){
        executor.execute(() -> {
            String c = code == null ? "" : code.trim();
            String p = phone == null ? "" : phone.trim();
            OrderCacheEntity entity = dao.findByCodeAndPhone(c, p);
            if (entity == null) entity = dao.findByCode(c);
            if (entity != null) {
                callback.onSuccess(OrderCacheMapper.toDomain(entity), false);
            } else {
                callback.onError(ErrorCode.NOT_FOUND, "Không tìm thấy bản lưu ngoại tuyến của đơn hàng.");
            }
        });
    }
    @Override public void getOrder(String id,LifecycleCallback callback){
        executor.execute(() -> {
            String cleanId = id == null ? "" : id.trim();
            OrderCacheEntity entity = dao.find(cleanId);
            if (entity == null) entity = dao.findByCode(cleanId);
            if (entity != null) {
                callback.onSuccess(OrderCacheMapper.toDomain(entity),
                        OrderCacheMapper.toPaymentDomain(entity),
                        OrderCacheMapper.toShipmentDomain(entity), false);
            } else {
                callback.onError(ErrorCode.NOT_FOUND, "Không tìm thấy dữ liệu ngoại tuyến cho đơn này.");
            }
        });
    }
    @Override public void getOrderItems(String id, ItemCallback callback) {
        executor.execute(() -> {
            String cleanId = id == null ? "" : id.trim();
            OrderCacheEntity entity = dao.find(cleanId);
            if (entity == null) entity = dao.findByCode(cleanId);
            if (entity != null) {
                Order order = OrderCacheMapper.toDomain(entity);
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    callback.success(order.getItems());
                    return;
                }
            }
            callback.error("Chưa có danh sách mặt hàng lưu ngoại tuyến.");
        });
    }
    @Override public void observeOrder(String id,LifecycleCallback callback){
        getOrder(id, callback);
    }
    @Override public void stopObservingOrder(String id) { }
    @Override public void transitionOrder(String id,OrderStatus target,LifecycleCallback callback){callback.onError(ErrorCode.NETWORK,WRITE_MESSAGE);}
    @Override public void confirmBankTransfer(String id,String reference,LifecycleCallback callback){callback.onError(ErrorCode.NETWORK,WRITE_MESSAGE);}
    @Override public void close(){executor.shutdownNow();}
}
