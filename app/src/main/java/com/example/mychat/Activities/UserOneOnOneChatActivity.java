package com.example.mychat.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.mychat.Adapter.MessagesAdapter;
import com.example.mychat.Model.Message;
import com.example.mychat.R;
import com.example.mychat.databinding.ActivityUserOneOnOneChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserOneOnOneChatActivity extends AppCompatActivity {

     ActivityUserOneOnOneChatBinding binding;

     MessagesAdapter adapter;

     ArrayList<Message> messages;

     String senderRoom,receiverRoom;

     FirebaseDatabase database;

     ActivityResultLauncher<Intent> gallaryLauncher ;

     Uri selectedImage;


      FirebaseStorage storage;

      ProgressDialog dialog;

      String senderUid;

      String receiveUid;

      ActivityResultLauncher cameraLauncher;

      FirebaseAuth auth;

      String userName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserOneOnOneChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);



         String name = getIntent().getStringExtra("name");
         String profile = getIntent().getStringExtra("image");
        String token = getIntent().getStringExtra("token");

       // Toast.makeText(this,token, Toast.LENGTH_SHORT).show();


        binding.name.setText(name);
         if (profile.equals("No Image"))
         {
            binding.circleImage.setImageResource(R.drawable.profile);
         }
         else {
            // binding.name.setText(name);
             Glide.with(this).load(profile)
                     .into(binding.circleImage);
         }

        binding.imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

         receiveUid = getIntent().getStringExtra("uid");
         senderUid = FirebaseAuth.getInstance().getUid();

        Log.d("TAG", "onCreate: uid "+ receiveUid);
        Log.d("TAG", "onCreate: name "+name);
        Log.d("TAG", "onCreate: senderuid"+ senderUid);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image....");
        dialog.setCancelable(false);

        registerActivityLauncherForGallary();
        registerActivityForCamera();


        messages = new ArrayList<>();


         database = FirebaseDatabase.getInstance();
         storage = FirebaseStorage.getInstance();
         auth = FirebaseAuth.getInstance();

         database.getReference().child("users").child(auth.getUid())
                         .addListenerForSingleValueEvent(new ValueEventListener() {
                             @Override
                             public void onDataChange(@NonNull DataSnapshot snapshot) {
                                 if (snapshot.exists())
                                 {
                                     userName = snapshot.child("name").getValue(String.class);
                                 }
                             }

                             @Override
                             public void onCancelled(@NonNull DatabaseError error) {

                             }
                         });



         database.getReference().child("presenence").child(receiveUid)
                 .addValueEventListener(new ValueEventListener() {
                     @Override
                     public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            String status = snapshot.getValue(String.class);
                            if(!status.isEmpty())
                            {
                                if (status.equals("Offline"))
                                {
                                    binding.profileSetting.setVisibility(View.GONE);
                                }
                                else
                                {
                                    binding.profileSetting.setText(status);
                                    binding.profileSetting.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                     }

                     @Override
                     public void onCancelled(@NonNull DatabaseError error) {

                     }
                 });

         senderRoom = senderUid + receiveUid;
         receiverRoom = receiveUid + senderUid;


        adapter = new MessagesAdapter(this,messages,senderRoom,receiverRoom);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));



        database.getReference().child("chats")
                         .child(senderRoom)
                         .child("messages")
                                 .addValueEventListener(new ValueEventListener() {
                                     @Override
                                     public void onDataChange(@NonNull DataSnapshot snapshot) {

                                         messages.clear();
                                         for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                             Message message = snapshot1.getValue(Message.class);
                                             message.setMessageId(snapshot1.getKey());
                                             messages.add(message);
                                         }

                                         adapter.notifyDataSetChanged();

                                         if (!messages.isEmpty()) {
                                             // Scroll smoothly to the last message
                                             int lastMessageIndex = messages.size() - 1;
                                             binding.recyclerView.smoothScrollToPosition(lastMessageIndex);
                                         }



                                     }

                                     @Override
                                     public void onCancelled(@NonNull DatabaseError error) {

                                     }
                                 });

         binding.imgSent.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {

                 String messageTxt = binding.edtMessageBox.getText().toString();

                 Date date = new Date();
                 Message message =new Message(messageTxt,senderUid,date.getTime());
                 binding.edtMessageBox.setText("");

                 String randomKey = database.getReference().push().getKey();

                 HashMap<String,Object> lastMsgObj = new HashMap<>();

                 lastMsgObj.put("lastMsg",message.getMessage());
                 lastMsgObj.put("lastMsgTime",date.getTime());

                 database.getReference()
                         .child("chats")
                         .child(senderRoom)
                         .updateChildren(lastMsgObj);

                 database.getReference()
                         .child("chats")
                         .child(receiverRoom)
                         .updateChildren(lastMsgObj);



                 database.getReference().child("chats")
                         .child(senderRoom)
                         .child("messages")
                        // .push()
                         .child(randomKey)
                         .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                             @Override
                             public void onSuccess(Void unused) {

                                 database.getReference().child("chats")
                                         .child(receiverRoom)
                                         .child("messages")
                                       //  .push()
                                         .child(randomKey)
                                         .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                             @Override
                                             public void onSuccess(Void unused) {
                                                 sendNotificatin(userName,message.getMessage(),token);
                                             }
                                         });


                             }
                         });
             }
         });

        binding.imgAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

             Intent intent = new Intent();
             intent.setAction(Intent.ACTION_GET_CONTENT);
             intent.setType("image/*");
             gallaryLauncher.launch(intent);

            }
        });

        binding.imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(UserOneOnOneChatActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(UserOneOnOneChatActivity.this
                            , new String[]{Manifest.permission.CAMERA},20);
                } else {
                    // Permission is granted, proceed to open the camera
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(cameraIntent);
                }
            }


        });

         final  Handler handler = new Handler();

        //text status that is it change
        binding.edtMessageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                database.getReference().child("presenence").child(senderUid).setValue("Typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStopTyping, 1000);
            }

            Runnable userStopTyping = new Runnable() {
                @Override
                public void run() {

                    database.getReference().child("presenence").child(senderUid).setValue("Online");

                }
            };

        });

        getSupportActionBar().setDisplayShowTitleEnabled(false);

      //  getSupportActionBar().setTitle(name);

        //back button
       //  getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    void sendNotificatin(String name,String message,String token)
    {
        try {
            //call api through volley
            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject data = new JSONObject();

            data.put("title", name);
            data.put("body", message);

            JSONObject notificationData = new JSONObject();
            notificationData.put("notification",data);
            notificationData.put("to",token);

            JsonObjectRequest request = new JsonObjectRequest(url, notificationData
                    , new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                   // Toast.makeText(UserOneOnOneChatActivity.this, "success", Toast.LENGTH_SHORT).show();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Toast.makeText(UserOneOnOneChatActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String,String> map =new HashMap<>();
                    String key = "Key=AAAAcJNEXjY:APA91bFh16ZeZeXeLwMbM9IxIhQkDLEhY1_Kal_dX2UnltjOzqSUYuENDtubdJZknjffdw6hBu-YzoJRjWLJDAEFq3QpJw_pVmX_zHeObqpFUBZsBlA2ApCR6Ky6ai9NnASryi_khFWp";
                    map.put("Authorization",key);
                    map.put("ContentType","application/json");
                    return map;
                }
            };

              queue.add(request);
        }
        catch (Exception ex)
        {

        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        String uid = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presenence").child(uid).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String uid = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presenence").child(uid).setValue("Offline");
        super.onStop();
    }

    //back work

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

 public  void  registerActivityLauncherForGallary()
 {
    gallaryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
            , new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    int resultcode = result.getResultCode();
                    Intent data = result.getData();

                    if (resultcode == RESULT_OK && data != null)
                    {
                       selectedImage = data.getData();
                        Calendar calendar = Calendar.getInstance();
                        StorageReference reference = storage.getReference().child("chats")
                                .child(calendar.getTimeInMillis()+"");
                        dialog.show();
                        reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                dialog.dismiss();

                                if (task.isSuccessful())
                                {
                                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            String filePath = uri.toString();

                                            String messageTxt = binding.edtMessageBox.getText().toString();

                                            Date date = new Date();
                                            Message message =new Message(messageTxt,senderUid,date.getTime());
                                            message.setMessage("Photo");
                                            message.setImageUrl(filePath);
                                            binding.edtMessageBox.setText("");

                                            String randomKey = database.getReference().push().getKey();

                                            HashMap<String,Object> lastMsgObj = new HashMap<>();

                                            lastMsgObj.put("lastMsg",message.getMessage());
                                            lastMsgObj.put("lastMsgTime",date.getTime());

                                            database.getReference()
                                                    .child("chats")
                                                    .child(senderRoom)
                                                    .updateChildren(lastMsgObj);

                                            database.getReference()
                                                    .child("chats")
                                                    .child(receiverRoom)
                                                    .updateChildren(lastMsgObj);



                                            database.getReference().child("chats")
                                                    .child(senderRoom)
                                                    .child("messages")
                                                    // .push()
                                                    .child(randomKey)
                                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {

                                                            database.getReference().child("chats")
                                                                    .child(receiverRoom)
                                                                    .child("messages")
                                                                    //  .push()
                                                                    .child(randomKey)
                                                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void unused) {

                                                                        }
                                                                    });


                                                        }
                                                    });

                                         //   Toast.makeText(UserOneOnOneChatActivity.this, filePath, Toast.LENGTH_SHORT).show();



                                        }
                                    });
                                }
                            }
                        });


                    }

                }
            });

 }

    public  void registerActivityForCamera() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {

                        int resultcode = result.getResultCode();
                        Intent data = result.getData();
                        if (resultcode == RESULT_OK && data != null) {

                            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] imageData = baos.toByteArray();

                            // Generate a unique filename for the image
                          Calendar calendar =Calendar.getInstance();

                            // Create a reference to the image file in the storage
                            StorageReference imageRef = storage.getReference().child("chats")
                                    .child(calendar.getTimeInMillis()+"");

                            dialog.show();
                            // Upload the image byte array to the storage
                            imageRef.putBytes(imageData)
                                       .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                    dialog.dismiss();

                                    if (task.isSuccessful()) {
                                        // Image uploaded successfully
                                        // Retrieve the download URL
                                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri downloadUri) {
                                                String imageUrl = downloadUri.toString();


                                                String messageTxt = binding.edtMessageBox.getText().toString();

                                                Date date = new Date();
                                                Message message =new Message(messageTxt,senderUid,date.getTime());
                                                message.setMessage("Photo");
                                                message.setImageUrl(imageUrl);
                                                binding.edtMessageBox.setText("");

                                                String randomKey = database.getReference().push().getKey();

                                                HashMap<String,Object> lastMsgObj = new HashMap<>();

                                                lastMsgObj.put("lastMsg",message.getMessage());
                                                lastMsgObj.put("lastMsgTime",date.getTime());

                                                database.getReference()
                                                        .child("chats")
                                                        .child(senderRoom)
                                                        .updateChildren(lastMsgObj);

                                                database.getReference()
                                                        .child("chats")
                                                        .child(receiverRoom)
                                                        .updateChildren(lastMsgObj);



                                                database.getReference().child("chats")
                                                        .child(senderRoom)
                                                        .child("messages")
                                                        // .push()
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void unused) {

                                                                database.getReference().child("chats")
                                                                        .child(receiverRoom)
                                                                        .child("messages")
                                                                        //  .push()
                                                                        .child(randomKey)
                                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void unused) {

                                                                            }
                                                                        });


                                                            }
                                                        });


                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(UserOneOnOneChatActivity.this,
                                                        e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {

                                        Toast.makeText(UserOneOnOneChatActivity.this, "uploading is fail", Toast.LENGTH_SHORT).show();

                                        // Handle the error case if the upload fails
                                    }
                                }
                            });
                        }
                    }
                });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ( requestCode == 20 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        }
    }
}