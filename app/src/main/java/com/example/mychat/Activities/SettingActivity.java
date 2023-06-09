package com.example.mychat.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mychat.Model.UserData;
import com.example.mychat.R;
import com.example.mychat.databinding.ActivitySettingBinding;
import com.example.mychat.databinding.EditProfileNameBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {

   ActivitySettingBinding binding;
   FirebaseDatabase database;
   FirebaseAuth auth;

   FirebaseStorage storage;

    UserData userData;

    ActivityResultLauncher<Intent> gallaryLauncher;

    private Bitmap selectImage;

    Uri dataImage;

   ProgressDialog dialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        auth = FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();



        dialog = new ProgressDialog(this);
        dialog.setMessage("uploadiag image");
        dialog.setCancelable(false);



         getUserInfo();

        registerActivityLauncher();

        binding.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                gallaryLauncher.launch(intent);

            }
        });

        binding.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                View view1 = LayoutInflater.from(SettingActivity.this).inflate(R.layout.edit_profile_name,null);
                EditProfileNameBinding binding1 = EditProfileNameBinding.bind(view1);
                AlertDialog dialog1 = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("Edit Name")
                        .setMessage("This is how you appear on MyChat,so pick a name your friend know by you")
                        .setView(view1)
                        .create();

               String name = binding.name.getText().toString();

               binding1.updateProfileName.setText(name);

              binding1.saveUpdateName.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {

                  String updateName = binding1.updateProfileName.getText().toString();
                  userData.setName(updateName);

                  HashMap<String,Object> UpdateNameObj = new HashMap<>();
                  UpdateNameObj.put("name",userData.getName());

                  database.getReference().child("users").child(auth.getUid())
                                  .updateChildren(UpdateNameObj);

                  binding.name.setText(userData.getName());
                  dialog1.dismiss();
                  }
              });

               binding1.Cancel.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {

                       dialog1.dismiss();
                   }
               });


                dialog1.show();

            }
        });





    }

    public void getUserInfo()
    {
        database.getReference().child("users").child(auth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                          userData = snapshot.getValue(UserData.class);

                            String name = userData.getName();
                            binding.name.setText(name);

                            String phoneNumber = userData.getPhoneNumber();
                            binding.UserphoneNumber.setText(phoneNumber);

                            if(userData.getProfileImage().equals("No Image"))
                            {
                                binding.profileImage.setImageResource(R.drawable.profile);

                            }

                            else
                            {

                                Glide.with(SettingActivity.this).load(userData.getProfileImage())
                                        .placeholder(R.drawable.placeholder)
                                        .into(binding.profileImage);

                            }
                        }

                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    public  void registerActivityLauncher()
    {
        gallaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {

                        int resultcode = result.getResultCode();
                        Intent data = result.getData();

                        if (resultcode == RESULT_OK && data != null) {

                            StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                            dialog.show();
                            reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            String imageUrl = uri.toString();

                                            userData.setProfileImage(imageUrl);

                                            HashMap<String, Object> ProfileImage = new HashMap<>();
                                            ProfileImage.put("profileImage",userData.getProfileImage());


                                            database.getReference().child("users")
                                                    .child(auth.getUid())
                                                    .updateChildren(ProfileImage);
                                            try {
                                                selectImage=MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData());
                                                binding.profileImage.setImageBitmap(selectImage);
                                                dialog.dismiss();
                                            }
                                            catch (IOException e) {
                                                dialog.dismiss();
                                                throw new RuntimeException(e);

                                            }




                                        }
                                    });
                                }
                            });

                        }
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
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}