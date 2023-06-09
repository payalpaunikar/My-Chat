package com.example.mychat.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


import com.example.mychat.databinding.ActivityOtpGenerateBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpGenerateActivity extends AppCompatActivity {

    ActivityOtpGenerateBinding binding;

    String phoneNumberCode;

    FirebaseAuth auth;

    String otpid;

    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        binding=ActivityOtpGenerateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //keyword open authomatically
        binding.edtOtp.requestFocus();

        phoneNumberCode=getIntent().getStringExtra("phoneNumber").toString();

        binding.txtPhoneNoGet.setText("Enter the OTP sent to "+phoneNumberCode);

        auth=FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending otp");
        dialog.setCancelable(false);
        dialog.show();

        initiateOtp();

        binding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (binding.edtOtp.getText().toString().isEmpty())
                {
                    Toast.makeText(OtpGenerateActivity.this, "Plese enter the otp", Toast.LENGTH_SHORT).show();
                }
                else if (binding.edtOtp.getText().toString().length()!=6) {

                    Toast.makeText(OtpGenerateActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                   // binding.txtPhoneNoGet.setText("");
                }
                else
                {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(otpid,binding.edtOtp.getText().toString());
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });




        binding.txtWrongNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OtpGenerateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        binding.txtResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.edtOtp.setText("");
                dialog.show();
                initiateOtp();
            }
        });


    }


   private  void initiateOtp()
    {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumberCode)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(OtpGenerateActivity.this)                 // (optional) Activity for callback binding
                        // If no activity is passed, reCAPTCHA verification can not be used.
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {

                               // super.onCodeSent(s, forceResendingToken);
                              dialog.dismiss();
                                otpid = s;
                            }

                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                                signInWithPhoneAuthCredential( phoneAuthCredential);
                                dialog.dismiss();

                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {

                                Toast.makeText(OtpGenerateActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                            }
                        })          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Intent i = new Intent(OtpGenerateActivity.this,RegisterActivity.class);
                            startActivity(i);
                            finishAffinity();

                        }
                        else {
                            Toast.makeText(OtpGenerateActivity.this, "OTP is incorrect", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }





}






