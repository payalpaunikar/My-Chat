package com.example.mychat.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mychat.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {


    ActivityMainBinding binding;

    FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.edtPhoneNo1.requestFocus();

        auth = FirebaseAuth.getInstance();


        if(auth.getCurrentUser() != null)
        {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
            finish();
        }


        binding.countryCodePicker1.registerCarrierNumberEditText(binding.edtPhoneNo1);

        binding.btnOtp.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {

                 Intent i =new Intent(MainActivity.this, OtpGenerateActivity.class);
                 i.putExtra("phoneNumber",binding.countryCodePicker1.getFullNumberWithPlus().replace(" ",""));
                 startActivity(i);
                 finish();

             }
         });



    }
}