package com.rescuefarm.service.auth;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class GoogleCredentialService {
    private static final String TAG = "GoogleCredentialService";
    private static final String WEB_CLIENT_ID_RESOURCE = "default_web_client_id";

    public interface Callback {
        void onIdToken(String idToken);
        void onError(String message);
    }

    public void requestGoogleIdToken(Activity activity, Callback callback) {
        int clientIdResource = activity.getResources().getIdentifier(
                WEB_CLIENT_ID_RESOURCE,
                "string",
                activity.getPackageName()
        );
        if (clientIdResource == 0) {
            callback.onError("Thiếu default_web_client_id. Hãy cập nhật google-services.json sau khi bật Google Sign-In.");
            return;
        }

        String serverClientId = activity.getString(clientIdResource);
        GetSignInWithGoogleOption googleSignInOption =
                new GetSignInWithGoogleOption.Builder(serverClientId).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleSignInOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(activity);
        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleCredential(result.getCredential(), callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException exception) {
                        Log.e(TAG, "Credential Manager sign-in failed", exception);
                        callback.onError(messageFor(exception));
                    }
                }
        );
    }

    private String messageFor(GetCredentialException exception) {
        if (exception instanceof GetCredentialCancellationException) {
            return "Bạn đã hủy đăng nhập Google.";
        }
        if (exception instanceof NoCredentialException) {
            return "Không tìm thấy tài khoản Google phù hợp trên thiết bị. Hãy thêm tài khoản Google rồi thử lại.";
        }
        String detail = exception.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = exception.getClass().getSimpleName();
        }
        return "Google Sign-In thất bại: " + detail;
    }

    private void handleCredential(Credential credential, Callback callback) {
        if (!(credential instanceof CustomCredential)
                || !TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            callback.onError("Credential nhận được không phải Google ID token.");
            return;
        }

        try {
            Bundle credentialData = ((CustomCredential) credential).getData();
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(credentialData);
            callback.onIdToken(googleCredential.getIdToken());
        } catch (RuntimeException exception) {
            callback.onError("Không thể đọc Google ID token. Hãy cập nhật ứng dụng và thử lại.");
        }
    }
}
