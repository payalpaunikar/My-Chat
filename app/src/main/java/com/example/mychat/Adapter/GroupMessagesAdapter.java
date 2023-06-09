package com.example.mychat.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mychat.Model.Message;
import com.example.mychat.Model.UserData;
import com.example.mychat.R;
import com.example.mychat.databinding.ItemReceiveGroupBinding;
import com.example.mychat.databinding.ItemSentGroupBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class GroupMessagesAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<Message> messages;

    final  int ITEM_SENT = 1;
    final int  ITEM_RECEIVE = 2;



    public GroupMessagesAdapter(Context context, ArrayList<Message> messages)
    {
        this.context =context;
        this.messages = messages;

    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType==ITEM_SENT)
        {
            View view = LayoutInflater.from(context).inflate(R.layout.item_sent_group,parent,false);
            return  new SentViewHolder(view);
        }
        else
        {
            View view = LayoutInflater.from(context).inflate(R.layout.item_receive_group,parent,false);
            return  new ReceiverHolder(view);
        }



    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if(FirebaseAuth.getInstance().getUid().equals(message.getSenderId()))
        {
            return  ITEM_SENT;
        }
        else
        {
            return ITEM_RECEIVE;
        }
      //  return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Message message = messages.get(position);

        int reaction[] = new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        };


        //recaction builder
        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reaction)
                .build();

        //reaction popup
        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {

            if (pos<0)
            {
                return  false;
            }

            if(holder.getClass() == SentViewHolder.class)
            {
                SentViewHolder viewHolder = (SentViewHolder)holder;
                viewHolder.binding.imgfeeling.setImageResource(reaction[pos]);
                viewHolder.binding.imgfeeling.setVisibility(View.VISIBLE);
            }
            else
            {
                ReceiverHolder viewHolder = (ReceiverHolder) holder;
                viewHolder.binding.imgfeeling.setImageResource(reaction[pos]);
                viewHolder.binding.imgfeeling.setVisibility(View.VISIBLE);
            }

            message.setFeeling(pos);

            FirebaseDatabase.getInstance().getReference()
                    .child("public")
                    .child(message.getMessageId())
                    .setValue(message);


            return true; // true is closing popup, false is requesting a new selection

        });


        if(holder.getClass() == SentViewHolder.class)
        {
            SentViewHolder viewHolder = (SentViewHolder)holder;

            if (message.getMessage().equals("Photo"))
            {
                viewHolder.binding.image.setVisibility(View.VISIBLE);
                viewHolder.binding.txtMessage.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl())
                                   .placeholder(R.drawable.placeholder)
                                   .into(viewHolder.binding.image);
            }
            else
            {
                 viewHolder.binding.txtMessage.setVisibility(View.VISIBLE);
                 viewHolder.binding.image.setVisibility(View.GONE);
            }

            FirebaseDatabase.getInstance().getReference().child("users")
                            .child(message.getSenderId())
                             .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists())
                                            {
                                                UserData user = snapshot.getValue(UserData.class);
                                                viewHolder.binding.name.setText("@"+ user.getName());
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });

            viewHolder.binding.txtMessage.setText(message.getMessage());

            if (message.getFeeling() >= 0) {
              //  message.setFeeling(reaction[(int) message.getFeeling()]);
                viewHolder.binding.imgfeeling.setImageResource(reaction[message.getFeeling()]);
                viewHolder.binding.imgfeeling.setVisibility(View.VISIBLE);
            }
            else
            {
                viewHolder.binding.imgfeeling.setVisibility(View.GONE);
            }

            viewHolder.binding.txtMessage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });

            viewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });






        }
        else
        {
           ReceiverHolder  viewHolder = (ReceiverHolder) holder;
            if (message.getMessage().equals("Photo"))
            {
                viewHolder.binding.image.setVisibility(View.VISIBLE);
                viewHolder.binding.txtMessage.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl())
                                   .placeholder(R.drawable.placeholder)
                                   .into(viewHolder.binding.image);
            }
            else
            {
                viewHolder.binding.txtMessage.setVisibility(View.VISIBLE);
                viewHolder.binding.image.setVisibility(View.GONE);
            }

            FirebaseDatabase.getInstance().getReference().child("users")
                    .child(message.getSenderId())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists())
                            {
                                UserData user = snapshot.getValue(UserData.class);
                                viewHolder.binding.name.setText("@"+ user.getName());

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            viewHolder.binding.txtMessage.setText(message.getMessage());

            if (message.getFeeling() >= 0) {
               // message.setFeeling(reaction[(int) message.getFeeling()]);
                viewHolder.binding.imgfeeling.setImageResource(reaction[message.getFeeling()]);
                viewHolder.binding.imgfeeling.setVisibility(View.VISIBLE);
            }
            else
            {
                viewHolder.binding.imgfeeling.setVisibility(View.GONE);
            }

            viewHolder.binding.txtMessage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });

            viewHolder.binding.image.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class SentViewHolder extends RecyclerView.ViewHolder {
        ItemSentGroupBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemSentGroupBinding.bind(itemView);

        }
    }

      public  class  ReceiverHolder extends  RecyclerView.ViewHolder
      {
        ItemReceiveGroupBinding binding;

          public ReceiverHolder(@NonNull View itemView) {
              super(itemView);

               binding = ItemReceiveGroupBinding.bind(itemView);
          }
      }
}
