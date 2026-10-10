package com.rescuefarm.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.rescuefarm.R;
import com.rescuefarm.domain.model.Banner;
import com.rescuefarm.domain.model.Category;
import com.rescuefarm.domain.model.Product;
import com.rescuefarm.domain.model.RescueCampaign;
import com.rescuefarm.domain.model.Post;
import com.rescuefarm.ui.feed.PostCardRenderer;
import com.rescuefarm.ui.feed.PostViewModel;
import com.rescuefarm.ui.feed.PostViewModelFactory;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GuestHomeFragment extends Fragment {
    private HomeViewModel viewModel;
    private PostViewModel postViewModel;
    private HomeCardRenderer renderer;
    private PostCardRenderer postRenderer;
    private TextView message;
    private TextView greeting, locationLabel;
    private ProgressBar progress;
    private TextView feedTitle;
    private LinearLayout bannerSection, criticalSection, valueSection, campaignSection, categorySection, feedSection;
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissions);

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guest_home, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state); renderer = new HomeCardRenderer(this);
        postRenderer = new PostCardRenderer(this);
        message = view.findViewById(R.id.homeMessage); progress = view.findViewById(R.id.homeProgress);
        greeting = view.findViewById(R.id.homeGreeting);
        locationLabel = view.findViewById(R.id.homeLocationLabel);
        bannerSection = view.findViewById(R.id.bannerSection);
        criticalSection = view.findViewById(R.id.criticalSection);
        valueSection = view.findViewById(R.id.valueSection);
        campaignSection = view.findViewById(R.id.campaignSection);
        categorySection = view.findViewById(R.id.categorySection);
        feedTitle = view.findViewById(R.id.feedTitle);
        feedSection = view.findViewById(R.id.feedSection);
        viewModel = new ViewModelProvider(this, new HomeViewModelFactory(requireContext()))
                .get(HomeViewModel.class);
        postViewModel = new ViewModelProvider(this, new PostViewModelFactory(requireContext()))
                .get(PostViewModel.class);
        viewModel.getHomeState().observe(getViewLifecycleOwner(), this::render);
        postViewModel.getFeed().observe(getViewLifecycleOwner(), this::renderFeed);
        viewModel.getGreetingName().observe(getViewLifecycleOwner(), name ->
                greeting.setText(getString(R.string.home_greeting_format,
                        name == null || name.trim().isEmpty() ? "bạn" : name.trim())));

        view.findViewById(R.id.searchButton).setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_guestHomeFragment_to_discoveryFragment));
        view.findViewById(R.id.notificationButton).setOnClickListener(unused ->
                Navigation.findNavController(view).navigate(viewModel.isAuthenticated()
                        ? R.id.action_guestHomeFragment_to_notificationFragment
                        : R.id.action_guestHomeFragment_to_loginFragment));
        view.findViewById(R.id.refreshHomeButton).setOnClickListener(v -> viewModel.refresh());
        locationLabel.setOnClickListener(v -> requestLocation());
        viewModel.refresh(); postViewModel.refreshFeed();
    }

    private void requestLocation() {
        boolean fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) viewModel.requestNearbyLocation();
        else permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION});
    }
    private void onPermissions(Map<String, Boolean> values) {
        if (Boolean.TRUE.equals(values.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(values.get(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            viewModel.requestNearbyLocation();
        } else viewModel.onLocationPermissionDenied();
    }

    private void render(HomeViewState state) {
        String prefix = state.getDataFreshness() == HomeViewState.DataFreshness.OFFLINE ? "OFFLINE • "
                : state.getDataFreshness() == HomeViewState.DataFreshness.STALE ? "CACHE CŨ • " : "";
        message.setText(prefix + state.getMessage()); progress.setVisibility(state.isRefreshing() ? View.VISIBLE : View.GONE);
        locationLabel.setText(state.getLocationState() == HomeViewState.LocationState.AVAILABLE
                ? R.string.home_location_active : state.getLocationState() == HomeViewState.LocationState.LOADING
                ? R.string.home_location_loading : R.string.home_location_default);
        renderBanners(state.getBanners());
        renderCampaigns(criticalSection, state.getCritical(), state, "Chưa có chiến dịch CRITICAL.");
        renderProducts(state.getValueProducts());
        List<RescueCampaign> otherCampaigns = new ArrayList<>();
        for (RescueCampaign campaign : state.getActiveCampaigns()) {
            if (campaign.getUrgencyLevel() != com.rescuefarm.domain.enums.UrgencyLevel.CRITICAL) {
                otherCampaigns.add(campaign);
            }
        }
        renderCampaigns(campaignSection, otherCampaigns, state, "Chưa có chiến dịch khác đang diễn ra.");
        renderCategories(state.getCategories());
    }
    private void renderFeed(List<Post> values) {
        feedSection.removeAllViews();
        List<Post> safe = values == null ? new ArrayList<>() : values;
        feedTitle.setVisibility(safe.isEmpty() ? View.GONE : View.VISIBLE);
        feedSection.setVisibility(safe.isEmpty() ? View.GONE : View.VISIBLE);
        int count = Math.min(4, safe.size());
        addGrid(feedSection, safe.subList(0, count), post -> postRenderer.homeCard(post,
                v -> openPost(post.getId())));
        if (!safe.isEmpty()) {
            MaterialButton all = new MaterialButton(requireContext());
            all.setText(R.string.open_feed_action);
            all.setTextColor(requireContext().getColor(R.color.rescue_primary));
            all.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.rescue_primary_container)));
            all.setElevation(0);
            all.setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(
                    R.id.action_guestHomeFragment_to_feedFragment));
            feedSection.addView(all, new LinearLayout.LayoutParams(-1, dp(40)));
        }
    }
    private void renderBanners(List<Banner> values) {
        bannerSection.removeAllViews();
        if (values.isEmpty()) { bannerSection.addView(renderer.message("Chưa có banner đang hiệu lực.")); return; }
        for (Banner value : values) {
            View card = renderer.banner(value, v -> {
                if (!value.getCampaignId().isEmpty()) openCampaign(value.getCampaignId());
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(250), -2);
            params.setMargins(0, dp(4), dp(10), dp(2));
            bannerSection.addView(card, params);
        }
    }
    private void renderCampaigns(LinearLayout container, List<RescueCampaign> values,
            HomeViewState state, String empty) {
        container.removeAllViews();
        if (values.isEmpty()) { container.addView(renderer.message(empty)); return; }
        addGrid(container, values, value -> renderer.campaign(value,
                state.distanceFor(value.getId()), v -> openCampaign(value.getId())));
    }
    private void renderProducts(List<Product> values) {
        valueSection.removeAllViews();
        if (values.isEmpty()) { valueSection.addView(renderer.message("Chưa có nông sản giảm giá trong cache.")); return; }
        addGrid(valueSection, values, value -> renderer.product(value, v -> openProduct(value.getId())));
    }
    private void renderCategories(List<Category> values) {
        categorySection.removeAllViews();
        if (values.isEmpty()) { categorySection.addView(renderer.message("Chưa có danh mục trong cache.")); return; }
        addGrid(categorySection, values, value -> renderer.category(value, v -> {
            Bundle args = new Bundle(); args.putString("categoryId", value.getId());
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_guestHomeFragment_to_discoveryFragment, args);
        }));
    }

    private <T> void addGrid(LinearLayout container, List<T> values, Function<T, View> createView) {
        int horizontalGap = dp(6);
        for (int index = 0; index < values.size(); index += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 0, 0, dp(2));
            container.addView(row, rowParams);

            View first = createView.apply(values.get(index));
            LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(0, -2, 1f);
            firstParams.setMargins(0, 0, horizontalGap, 0);
            row.addView(first, firstParams);
            if (index + 1 < values.size()) {
                View second = createView.apply(values.get(index + 1));
                row.addView(second, new LinearLayout.LayoutParams(0, -2, 1f));
            } else {
                row.addView(new Space(requireContext()), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
    }
    private void openCampaign(String id) {
        Bundle args = new Bundle(); args.putString("campaignId", id);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_guestHomeFragment_to_campaignDetailFragment, args);
    }
    private void openProduct(String id) {
        Bundle args = new Bundle(); args.putString("productId", id);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_guestHomeFragment_to_productDetailFragment, args);
    }
    private void openPost(String id) {
        Bundle args = new Bundle(); args.putString("postId", id);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_guestHomeFragment_to_postDetailFragment, args);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
