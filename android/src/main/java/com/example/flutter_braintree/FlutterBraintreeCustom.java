package com.example.flutter_braintree;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;

import java.util.HashMap;

public class FlutterBraintreeCustom extends AppCompatActivity implements PaymentMethodNonceCreatedListener, BraintreeCancelListener, BraintreeErrorListener, BraintreeResponseListener<String> {
    private BraintreeFragment braintreeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flutter_braintree_custom);
        try {
            Intent intent = getIntent();
            braintreeFragment = BraintreeFragment.newInstance(this, intent.getStringExtra("authorization"));
            String type = intent.getStringExtra("type");
            if (type.equals("tokenizeCreditCard")) {
                tokenizeCreditCard();
            } else if (type.equals("requestPaypalNonce")) {
                requestPaypalNonce();
            } else if (type.equals("threeDSecure")) {
                threeDSecureRequest();
            } else if (type.equals("requestDeviceData")) {
                requestDeviceData();
            } else if (type.equals("requestPayPalDeviceData")) {
                requestPayPalDeviceData();
            } else {
                throw new Exception("Invalid request type: " + type);
            }
        } catch (Exception e) {
            Intent result = new Intent();
            result.putExtra("error", e);
            setResult(2, result);
            finish();
            return;
        }
    }

    protected void requestDeviceData() {
        DataCollector.collectDeviceData(braintreeFragment, this);
    }

    protected void requestPayPalDeviceData() {
        DataCollector.collectPayPalDeviceData(braintreeFragment, this);
    }

    protected void threeDSecureRequest() {
        Intent intent = getIntent();

        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
                .amount(intent.getStringExtra("amount"))
                .nonce(intent.getStringExtra("nonce"));

        ThreeDSecure.performVerification(braintreeFragment, threeDSecureRequest);
    }

    protected void tokenizeCreditCard() {
        Intent intent = getIntent();
        CardBuilder builder = new CardBuilder()
                .cardNumber(intent.getStringExtra("cardNumber"))
                .expirationMonth(intent.getStringExtra("expirationMonth"))
                .expirationYear(intent.getStringExtra("expirationYear"))
                .validate(intent.getBooleanExtra("shouldValidate", false));

        if (intent.getStringExtra("cvv") != null) {
            builder = builder.cvv(intent.getStringExtra("cvv"));
        }

        Card.tokenize(braintreeFragment, builder);
    }

    protected void requestPaypalNonce() {
        Intent intent = getIntent();
        PayPalRequest request = new PayPalRequest(intent.getStringExtra("amount"))
                .currencyCode(intent.getStringExtra("currencyCode"))
                .displayName(intent.getStringExtra("displayName"))
                .billingAgreementDescription(intent.getStringExtra("billingAgreementDescription"))
                .intent(PayPalRequest.INTENT_AUTHORIZE);

        if (intent.getStringExtra("amount") == null) {
            // Vault flow
            PayPal.requestBillingAgreement(braintreeFragment, request);
        } else {
            // Checkout flow
            PayPal.requestOneTimePayment(braintreeFragment, request);
        }
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        HashMap<String, Object> nonceMap = new HashMap<String, Object>();
        nonceMap.put("nonce", paymentMethodNonce.getNonce());
        nonceMap.put("typeLabel", paymentMethodNonce.getTypeLabel());
        nonceMap.put("description", paymentMethodNonce.getDescription());
        nonceMap.put("isDefault", paymentMethodNonce.isDefault());

        Intent result = new Intent();
        result.putExtra("type", "paymentMethodNonce");
        result.putExtra("paymentMethodNonce", nonceMap);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onCancel(int requestCode) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onError(Exception error) {
        Intent result = new Intent();
        result.putExtra("error", error);
        setResult(2, result);
        finish();
    }

    @Override
    public void onResponse(String deviceData) {
        HashMap<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("deviceData", deviceData);

        Intent result = new Intent();
        result.putExtra("type", "deviceDataResponse");
        result.putExtra("deviceData", responseMap);

        setResult(RESULT_OK, result);
        finish();
    }
}
