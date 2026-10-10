package com.rescuefarm.ui.home;

import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.rescuefarm.domain.model.Banner;
import com.rescuefarm.domain.model.Category;
import com.rescuefarm.domain.model.Product;
import com.rescuefarm.domain.model.RescueCampaign;
import java.text.NumberFormat;
import java.util.Locale;

public final class HomeCardRenderer {
    private final Fragment fragment;
    public HomeCardRenderer(Fragment fragment) { this.fragment = fragment; }

    public View banner(Banner value, View.OnClickListener listener) {
        MaterialCardView card = card(18);
        LinearLayout body = body();
        body.setPadding(dp(8), dp(8), dp(8), dp(9));
        ImageView image = image(84);
        if (!value.getImageUrl().isEmpty()) Glide.with(fragment).load(value.getImageUrl()).centerCrop().into(image);
        body.addView(imageFrame(image, 14, 84));
        TextView title = text(value.getTitle(), 13, true);
        title.setMaxLines(2); title.setPadding(dp(3), dp(7), dp(3), 0);
        body.addView(title);
        card.addView(body); card.setOnClickListener(listener); return card;
    }

    public View campaign(RescueCampaign value, double distance, View.OnClickListener listener) {
        MaterialCardView card = card(16); LinearLayout body = body();
        body.setPadding(dp(10), dp(10), dp(10), dp(10));
        TextView title = text(value.getTitle(), 14, true); title.setMaxLines(2); body.addView(title);
        TextView badge = text(value.getHighlightLabel() + "  ·  " + value.getUrgencyLevel().name(), 10, true);
        badge.setTextColor(fragment.getResources().getColor(value.getUrgencyLevel()
                == com.rescuefarm.domain.enums.UrgencyLevel.CRITICAL
                ? com.rescuefarm.R.color.rescue_critical : com.rescuefarm.R.color.rescue_primary,
                fragment.requireContext().getTheme()));
        badge.setPadding(0, dp(5), 0, dp(3)); body.addView(badge);
        body.addView(text(String.format(Locale.forLanguageTag("vi-VN"),
                "%.0f / %.0f %s", value.getRescuedQuantity(), value.getTargetQuantity(),
                value.getRescueMode().name().equals("MOBILE_POINT") ? "di động" : "cố định"), 11, false));
        if (Double.isFinite(distance)) body.addView(text(String.format(Locale.forLanguageTag("vi-VN"),
                "Cách bạn %.1f km", distance), 11, false));
        card.addView(body); card.setOnClickListener(listener); return card;
    }

    public View product(Product value, View.OnClickListener listener) {
        MaterialCardView card = card(16); LinearLayout row = body();
        row.setPadding(dp(8), dp(8), dp(8), dp(9));
        ImageView image = image(82);
        if (!value.getImageUrls().isEmpty()) Glide.with(fragment).load(value.getImageUrls().get(0)).centerCrop().into(image);
        row.addView(imageFrame(image, 12, 82));
        TextView name = text(value.getName(), 13, true); name.setMaxLines(2);
        name.setMinLines(2); name.setPadding(dp(2), dp(7), dp(2), 0); row.addView(name);
        TextView price = text(money(value.getRescuePrice()) + " / " + value.getUnit(), 12, true);
        price.setTextColor(fragment.getResources().getColor(com.rescuefarm.R.color.rescue_primary,
                fragment.requireContext().getTheme())); row.addView(price);
        TextView discount = text("Giảm " + Math.round(value.calculateDiscountPercent()) + "%", 11, false);
        discount.setTextColor(fragment.getResources().getColor(com.rescuefarm.R.color.rescue_critical,
                fragment.requireContext().getTheme())); row.addView(discount);
        card.addView(row);
        card.setOnClickListener(listener); return card;
    }

    public View category(Category value, View.OnClickListener listener) {
        MaterialCardView card = card(16); LinearLayout body = body();
        card.setCardBackgroundColor(fragment.getResources().getColor(
                com.rescuefarm.R.color.rescue_primary_container, fragment.requireContext().getTheme()));
        body.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(8), dp(6), dp(8), dp(6));
        if (!value.getImageUrl().isEmpty()) {
            ImageView image = image(48);
            Glide.with(fragment).load(value.getImageUrl()).centerCrop().into(image);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            imageParams.setMarginEnd(dp(9)); body.addView(imageFrame(image, 14, 48), imageParams);
        }
        TextView name = text(value.getName(), 12, true);
        name.setMaxLines(2); body.addView(name); card.addView(body);
        card.setOnClickListener(listener); return card;
    }

    public View message(String value) {
        TextView view = text(value, 14, false); view.setPadding(dp(4), dp(8), dp(4), dp(14)); return view;
    }
    private MaterialCardView card(int radius) {
        MaterialCardView card = new MaterialCardView(fragment.requireContext());
        card.setRadius(dp(radius)); card.setStrokeWidth(dp(1)); card.setStrokeColor(
                fragment.getResources().getColor(com.rescuefarm.R.color.rescue_outline, fragment.requireContext().getTheme()));
        card.setCardBackgroundColor(fragment.getResources().getColor(com.rescuefarm.R.color.rescue_surface,
                fragment.requireContext().getTheme()));
        card.setCardElevation(0); return card;
    }
    private MaterialCardView imageFrame(ImageView image, int radius, int height) {
        MaterialCardView frame = new MaterialCardView(fragment.requireContext());
        frame.setRadius(dp(radius)); frame.setStrokeWidth(0); frame.setCardElevation(0);
        frame.setCardBackgroundColor(fragment.getResources().getColor(
                com.rescuefarm.R.color.rescue_primary_container, fragment.requireContext().getTheme()));
        frame.addView(image, new FrameLayout.LayoutParams(-1, dp(height)));
        return frame;
    }
    private LinearLayout body() {
        LinearLayout body = new LinearLayout(fragment.requireContext()); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(14)); return body;
    }
    private ImageView image(int height) {
        ImageView image = new ImageView(fragment.requireContext()); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription("Ảnh nội dung giải cứu");
        image.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(height))); return image;
    }
    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(fragment.requireContext()); view.setText(value); view.setTextSize(size);
        if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD); return view;
    }
    private String money(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(value);
    }
    private int dp(int value) {
        return Math.round(value * fragment.getResources().getDisplayMetrics().density);
    }
}
