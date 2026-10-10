package com.rescuefarm.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.rescuefarm.R;
import com.rescuefarm.domain.enums.UserRole;

public class RegistrationChoiceFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration_choice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.customerRegistrationCard).setOnClickListener(unused -> openRegistration(UserRole.CUSTOMER));
        view.findViewById(R.id.sellerRegistrationCard).setOnClickListener(unused -> openRegistration(UserRole.SELLER));
        view.findViewById(R.id.registrationChoiceBackButton).setOnClickListener(unused -> {
            if (!NavHostFragment.findNavController(this).navigateUp()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_registrationChoice_to_login);
            }
        });
    }

    private void openRegistration(UserRole role) {
        Bundle arguments = new Bundle();
        arguments.putString("role", role.name());
        NavHostFragment.findNavController(this).navigate(
                R.id.action_registrationChoice_to_register,
                arguments
        );
    }
}
