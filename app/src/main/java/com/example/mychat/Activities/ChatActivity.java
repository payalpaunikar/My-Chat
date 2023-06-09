package com.example.mychat.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mychat.Adapter.UsersAdapter;
import com.example.mychat.Model.UserData;
import com.example.mychat.R;
import com.example.mychat.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;

    FirebaseDatabase database;
    ArrayList<UserData> userData;
    UsersAdapter usersAdapter ;

    ProgressDialog dialog;

     ProgressDialog progressDialog;

    UserData user;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                String toolBarColor = mFirebaseRemoteConfig.getString("toolbarColour");
                String BackroundImage = mFirebaseRemoteConfig.getString("BackgroundImage");
                // Toast.makeText(ChatActivity.this, toolBarColor, Toast.LENGTH_SHORT).show();
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(toolBarColor)));
            }
        });


        database = FirebaseDatabase.getInstance();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("token", token);
                        database.getReference().child("users")
                                .child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(map);
                        //  Toast.makeText(ChatActivity.this,token, Toast.LENGTH_SHORT).show();
                    }
                });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Your chat is visible...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);


        userData = new ArrayList<>();


        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(UserData.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        usersAdapter = new UsersAdapter(this,userData);
        binding.recyclerView.setAdapter(usersAdapter);

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userData.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    UserData userData1 = snapshot1.getValue(UserData.class);
                    if (!userData1.getUid().equals(FirebaseAuth.getInstance().getUid()))

                        userData.add(userData1);
                }


                progressDialog.dismiss();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
               switch (item.getItemId())
               {
                   case R.id.profile_setting:

                       Intent intent = new Intent(ChatActivity.this,SettingActivity.class);
                       startActivity(intent);
               }
                return false;
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        String uid = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presenence").child(uid).setValue("Online");
    }

    @Override
  protected void onPause() {
        if (auth.getCurrentUser() != null) {
            super.onPause();
            String uid = FirebaseAuth.getInstance().getUid();
            database.getReference().child("presenence").child(uid).setValue("Offline");
            super.onStop();
        }
        else
        {
            super.onPause();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {

            case R.id.groups:
                startActivity(new Intent(ChatActivity.this,GroupChatActivity.class));
                break;

            case R.id.logout:
                database.getReference().child("presenence").child(auth.getUid()).setValue("Offline");
                 auth.signOut();
                 Intent intent = new Intent(ChatActivity.this,MainActivity.class);
                 startActivity(intent);
                 finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}