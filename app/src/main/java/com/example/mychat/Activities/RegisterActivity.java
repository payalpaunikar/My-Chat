package com.example.mychat.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mychat.Model.UserData;
import com.example.mychat.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    ActivityRegisterBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;

    Uri dataImage;

    ActivityResultLauncher<Intent>  activityResultLauncherForProfile;

    private Bitmap selectImage;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        binding=ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();

        registerActivityForSelectedImage();


        dialog = new ProgressDialog(RegisterActivity.this);
        dialog.setMessage("Updating Profile.....");
        dialog.setCancelable(false);


        binding.imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

             if (ContextCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                          != PackageManager.PERMISSION_GRANTED)
             {
                 ActivityCompat.requestPermissions(RegisterActivity.this
                         ,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                    ,1);


             }
             else
             {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncherForProfile.launch(intent);
             }

            }
        });

        binding.btnGenerateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String name = binding.edtUserName.getText().toString();

                if (name.isEmpty())
                {
                    binding.edtUserName.setError("Please type your user name");
                    return;
                }

                dialog.show();

                if (dataImage != null)
                {
                    StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                    reference.putFile(dataImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful())
                            {
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        String imageUrl = uri.toString();
                                        String uid = auth.getUid();
                                        String phoneNumber = auth.getCurrentUser().getPhoneNumber();
                                        String name = binding.edtUserName.getText().toString();


                                        UserData userData = new UserData(uid,name,phoneNumber,imageUrl);

                                        database.getReference()
                                                .child("users")
                                                .child(uid)
                                                .setValue(userData)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {

                                                        dialog.dismiss();
                                                        Intent intent = new Intent(RegisterActivity.this, ChatActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    });
                }
                else
                {
                    String uid = auth.getUid();
                    String phoneNumber = auth.getCurrentUser().getPhoneNumber();



                    UserData userData = new UserData(uid,name,phoneNumber,"No Image");

                    database.getReference()
                            .child("users")
                            .child(uid)
                            .setValue(userData)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {

                                    dialog.dismiss();
                                    Intent intent = new Intent(RegisterActivity.this,ChatActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            }
        });

    }

    public  void registerActivityForSelectedImage()
    {
        activityResultLauncherForProfile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {

                        int resulcode = result.getResultCode();
                        Intent data = result.getData();

                         if (resulcode == RESULT_OK && data !=null)
                         {
                             try {
                                 selectImage = MediaStore.Images
                                         .Media.getBitmap(getContentResolver(),data.getData());

                                 binding.imgProfile.setImageBitmap(selectImage);

                                 dataImage = data.getData();
                             }
                             catch (IOException e) {
                                 throw new RuntimeException(e);
                             }
                         }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ( requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncherForProfile.launch(i);
        }
    }
}