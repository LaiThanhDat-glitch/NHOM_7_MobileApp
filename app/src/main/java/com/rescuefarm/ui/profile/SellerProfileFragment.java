package com.rescuefarm.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.rescuefarm.R;
import com.rescuefarm.domain.model.Seller;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.bumptech.glide.Glide;

public class SellerProfileFragment extends Fragment {
    private TextInputEditText representativeInput, shopNameInput, descriptionInput, avatarInput, addressInput;
    private ImageView sellerAvatarView;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_seller_profile, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        ProfileViewModel viewModel = new ViewModelProvider(this,
                new ProfileViewModelFactory(requireContext())).get(ProfileViewModel.class);
        representativeInput = view.findViewById(R.id.sellerRepresentativeInput);
        shopNameInput = view.findViewById(R.id.sellerShopNameInput);
        descriptionInput = view.findViewById(R.id.sellerDescriptionInput);
        avatarInput = view.findViewById(R.id.sellerAvatarInput);
        addressInput = view.findViewById(R.id.sellerAddressInput);
        sellerAvatarView = view.findViewById(R.id.sellerProfileAvatar);
        view.findViewById(R.id.openSellerApplicationButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerApplicationFragment));
        view.findViewById(R.id.openSellerDashboardButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerDashboardFragment));
        view.findViewById(R.id.openSellerProductsButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerProductListFragment));
        view.findViewById(R.id.openSellerCampaignsButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerCampaignListFragment));
        view.findViewById(R.id.openSellerPostsButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerPostListFragment));
        view.findViewById(R.id.openSellerOrdersButton).setOnClickListener(
                Navigation.createNavigateOnClickListener(
                        R.id.action_sellerProfileFragment_to_sellerOrderListFragment));
        view.findViewById(R.id.saveSellerProfileButton).setOnClickListener(v ->
                viewModel.updateSellerProfile(text(representativeInput), text(shopNameInput),
                        text(descriptionInput), text(avatarInput), text(addressInput)));
        view.findViewById(R.id.logoutButton).setOnClickListener(unused ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout_title)
                        .setMessage(R.string.logout_confirmation)
                        .setNegativeButton(R.string.cancel_action, null)
                        .setPositiveButton(R.string.logout_action, (dialog, which) -> {
                            viewModel.signOut();
                            NavController navController = NavHostFragment.findNavController(this);
                            NavOptions options = new NavOptions.Builder()
                                    .setPopUpTo(R.id.main_navigation, true)
                                    .build();
                            navController.navigate(R.id.loginFragment, null, options);
                        })
                        .show());
        viewModel.getState().observe(getViewLifecycleOwner(), value -> {
            if (value.getStatus() == ProfileScreenState.Status.ERROR) {
                Toast.makeText(requireContext(), value.getMessage(), Toast.LENGTH_LONG).show();
            } else if (value.getStatus() == ProfileScreenState.Status.PROFILE
                    && value.getUser() instanceof Seller) {
                Seller seller = (Seller) value.getUser();
                ((TextView) view.findViewById(R.id.sellerShopName)).setText(
                        seller.getShopName() == null || seller.getShopName().isEmpty()
                                ? getString(R.string.shop_not_updated) : seller.getShopName());
                ((TextView) view.findViewById(R.id.sellerStatus)).setText(seller.getSellerStatus().name());
                ((TextView) view.findViewById(R.id.sellerBusinessRule)).setText(seller.canSell()
                        ? R.string.seller_approved_message : R.string.seller_pending_message);
                representativeInput.setText(seller.getRepresentativeName());
                shopNameInput.setText(seller.getShopName());
                descriptionInput.setText(seller.getShopDescription());
                avatarInput.setText(seller.getShopAvatarUrl());
                addressInput.setText(seller.getAddress());
                String shopAvatarUrl = seller.getShopAvatarUrl();
                if (shopAvatarUrl != null && !shopAvatarUrl.trim().isEmpty()) {
                    sellerAvatarView.setPadding(0, 0, 0, 0);
                    sellerAvatarView.setImageTintList(null);
                    Glide.with(this).load(shopAvatarUrl.trim()).circleCrop()
                            .placeholder(R.drawable.ic_nav_profile).error(R.drawable.ic_nav_profile)
                            .into(sellerAvatarView);
                } else {
                    Glide.with(this).clear(sellerAvatarView);
                    sellerAvatarView.setPadding(16, 16, 16, 16);
                    sellerAvatarView.setImageResource(R.drawable.ic_nav_profile);
                    sellerAvatarView.setImageTintList(android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.rescue_primary)));
                }
            } else if (value.getStatus() == ProfileScreenState.Status.SAVED) {
                Toast.makeText(requireContext(), value.getMessage(), Toast.LENGTH_SHORT).show();
                viewModel.loadProfile();
            }
        });
        viewModel.loadProfile();
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
