package com.example.mychat.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mychat.Adapter.GroupMessagesAdapter;
import com.example.mychat.Model.Message;
import com.example.mychat.databinding.ActivityGroupChatBinding;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatBinding binding;

    GroupMessagesAdapter adapter;

    ArrayList<Message> messages;


    FirebaseDatabase database;

    FirebaseStorage storage;

    ProgressDialog dialog;

    String senderUid;
    ActivityResultLauncher<Intent> gallaryLauncher;

    Uri selectedImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Group Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image....");
        dialog.setCancelable(false);

        messages = new ArrayList<>();
        registerActivityLauncherForGallary();

        adapter= new GroupMessagesAdapter(this,messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database.getReference().child("public")
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

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        senderUid = FirebaseAuth.getInstance().getUid();

        binding.imgSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String messageTxt = binding.edtMessageBox.getText().toString();

                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime());
                binding.edtMessageBox.setText("");

                database.getReference().child("public")
                        .push()
                        .setValue(message);
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


                                               database.getReference().child("public")
                                                       .push()
                                                       .setValue(message);




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


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}