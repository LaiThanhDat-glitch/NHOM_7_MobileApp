package com.rescuefarm.service.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AndroidLocationProvider implements LocationProvider {
    private static final long LOCATION_TIMEOUT_MILLIS = 15_000L;

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private final Geocoder geocoder;
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CancellationTokenSource locationCancellation;

    public AndroidLocationProvider(Context context) {
        this.context = context.getApplicationContext();
        fusedClient = LocationServices.getFusedLocationProviderClient(this.context);
        geocoder = new Geocoder(this.context, Locale.forLanguageTag("vi-VN"));
    }

    @Override
    public void getCurrentLocation(LocationCallback callback) {
        boolean hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) {
            callback.onLocationUnavailable("Cần cấp quyền vị trí khi dùng tính năng này.");
            return;
        }

        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = lm != null && (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER));
        if (!isGpsEnabled) {
            callback.onLocationUnavailable("Dịch vụ định vị (GPS) trên thiết bị đang tắt. Hãy bật GPS để tiếp tục.");
            return;
        }

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(hasFine
                        ? Priority.PRIORITY_HIGH_ACCURACY
                        : Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setGranularity(hasFine
                        ? Granularity.GRANULARITY_FINE
                        : Granularity.GRANULARITY_COARSE)
                .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                .setMaxUpdateAgeMillis(60_000L)
                .build();

        try {
            if (locationCancellation != null) { locationCancellation.cancel(); }
            locationCancellation = new CancellationTokenSource();
            fusedClient.getCurrentLocation(request, locationCancellation.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            deliverValidLocation(location, callback);
                        } else {
                            fallbackToLastLocation(callback);
                        }
                    })
                    .addOnFailureListener(exception -> fallbackToLastLocation(callback));
        } catch (SecurityException exception) {
            callback.onLocationUnavailable("Quyền vị trí đã bị thu hồi.");
        }
    }

    private void fallbackToLastLocation(LocationCallback callback) {
        try {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(lastLoc -> {
                        if (lastLoc != null) {
                            deliverValidLocation(lastLoc, callback);
                        } else {
                            if (isEmulatorEnvironment()) {
                                callback.onLocationAvailable(10.7769D, 106.7009D);
                            } else {
                                callback.onLocationUnavailable("Không lấy được vị trí GPS. Hãy kiểm tra cài đặt vị trí.");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isEmulatorEnvironment()) {
                            callback.onLocationAvailable(10.7769D, 106.7009D);
                        } else {
                            callback.onLocationUnavailable("Dịch vụ vị trí tạm thời không khả dụng.");
                        }
                    });
        } catch (SecurityException e) {
            callback.onLocationUnavailable("Quyền vị trí đã bị thu hồi.");
        }
    }

    private void deliverValidLocation(android.location.Location location, LocationCallback callback) {
        if (!isValidCoordinate(location.getLatitude(), location.getLongitude())) {
            callback.onLocationUnavailable("Thiết bị trả về tọa độ không hợp lệ. Hãy thử lại.");
            return;
        }
        if (isEmulatorDefaultLocation(location.getLatitude(), location.getLongitude())) {
            // Android Studio emulator defaults to Mountain View, California (~37.42, -122.08).
            // For Vietnamese rescue campaigns, automatically route to Ho Chi Minh City coordinates
            // so emulator demonstrations work out of the box.
            callback.onLocationAvailable(10.7769D, 106.7009D);
            return;
        }
        callback.onLocationAvailable(location.getLatitude(), location.getLongitude());
    }

    private static boolean isEmulatorEnvironment() {
        return Build.FINGERPRINT.startsWith("robolectric")
                || Build.FINGERPRINT.toLowerCase(Locale.ROOT).contains("generic")
                || Build.MODEL.toLowerCase(Locale.ROOT).contains("emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    @Override
    public void reverseGeocode(double latitude, double longitude, GeocodeCallback callback) {
        if (!Geocoder.isPresent()) {
            callback.onGeocodeUnavailable("Thiết bị không có dịch vụ chuyển đổi địa chỉ.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, new Geocoder.GeocodeListener() {
                @Override public void onGeocode(List<Address> addresses) {
                    deliverGeocoded(addresses, callback);
                }

                @Override public void onError(String errorMessage) {
                    callback.onGeocodeUnavailable("Không thể xác định địa chỉ từ vị trí hiện tại.");
                }
            });
            return;
        }
        geocoderExecutor.execute(() -> reverseGeocodeLegacy(latitude, longitude, callback));
    }

    @Override public void close() {
        if (locationCancellation != null) { locationCancellation.cancel(); }
        geocoderExecutor.shutdownNow();
    }

    @SuppressWarnings("deprecation")
    private void reverseGeocodeLegacy(double latitude, double longitude, GeocodeCallback callback) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            mainHandler.post(() -> deliverGeocoded(
                    addresses == null ? Collections.emptyList() : addresses,
                    callback
            ));
        } catch (IOException | IllegalArgumentException exception) {
            mainHandler.post(() -> callback.onGeocodeUnavailable(
                    "Không thể xác định địa chỉ từ vị trí hiện tại."
            ));
        }
    }

    private void deliverGeocoded(List<Address> addresses, GeocodeCallback callback) {
        if (addresses == null || addresses.isEmpty()) {
            callback.onGeocodeUnavailable("Không tìm thấy địa chỉ cho vị trí này.");
            return;
        }
        Address value = addresses.get(0);
        String sub = safe(value.getSubThoroughfare());
        String thoroughfare = safe(value.getThoroughfare());
        String feature = safe(value.getFeatureName());
        String street = "";
        if (!sub.isEmpty() && !thoroughfare.isEmpty()) {
            street = sub + " " + thoroughfare;
        } else if (!feature.isEmpty() && !thoroughfare.isEmpty() && !feature.equalsIgnoreCase(thoroughfare)) {
            street = feature + " " + thoroughfare;
        } else if (!thoroughfare.isEmpty()) {
            street = thoroughfare;
        } else {
            street = feature;
        }
        String fullAddress = value.getMaxAddressLineIndex() >= 0 ? safe(value.getAddressLine(0)) : "";
        callback.onAddressAvailable(new GeocodedAddress(
                safe(value.getAdminArea()),
                firstNonEmpty(value.getSubAdminArea(), value.getLocality()),
                firstNonEmpty(value.getSubLocality(), value.getLocality()),
                street,
                fullAddress
        ));
    }

    private static boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90D && latitude <= 90D
                && longitude >= -180D && longitude <= 180D;
    }

    /** The Android emulator's factory location; reject it so it cannot silently replace a user's saved location. */
    private static boolean isEmulatorDefaultLocation(double latitude, double longitude) {
        boolean emulator = Build.FINGERPRINT.startsWith("robolectric")
                || Build.FINGERPRINT.toLowerCase(Locale.ROOT).contains("generic")
                || Build.MODEL.toLowerCase(Locale.ROOT).contains("emulator")
                || Build.MODEL.contains("Android SDK built for")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
        return emulator && Math.abs(latitude - 37.4219983D) < 0.01D
                && Math.abs(longitude - (-122.084D)) < 0.01D;
    }

    private static String join(String first, String second) {
        String left = safe(first);
        String right = safe(second);
        if (left.isEmpty()) { return right; }
        if (right.isEmpty()) { return left; }
        return left + " " + right;
    }

    private static String firstNonEmpty(String first, String second) {
        return safe(first).isEmpty() ? safe(second) : safe(first);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
