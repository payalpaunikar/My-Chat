package com.example.mychat.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mychat.Model.Message;
import com.example.mychat.R;
import com.example.mychat.databinding.DeleteDialogBinding;
import com.example.mychat.databinding.DeleteMesageBinding;
import com.example.mychat.databinding.IemSendBinding;
import com.example.mychat.databinding.ItemReceiveBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;


public class MessagesAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<Message> messages;

    final  int ITEM_SENT = 1;
    final int  ITEM_RECEIVE = 2;

    String senderRoom;
    String receverRoom;


    Boolean notShowFelling=false;



    public MessagesAdapter(Context context, ArrayList<Message> messages,String senderRoom,String receverRoom)
    {
        this.context =context;
        this.messages = messages;
        this.senderRoom = senderRoom;
        this.receverRoom = receverRoom;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType==ITEM_SENT)
        {
            View view = LayoutInflater.from(context).inflate(R.layout.iem_send,parent,false);
            return  new SentViewHolder(view);
        }
        else
        {
            View view = LayoutInflater.from(context).inflate(R.layout.item_receive,parent,false);
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
                return false;
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
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.getMessageId())
                    .setValue(message);


            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(receverRoom)
                    .child("messages")
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

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                  //  Log.d("TAG", "onLongClick: click");
                //    Toast.makeText(context, "message", Toast.LENGTH_SHORT).show();
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_dialog,null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(view1)
                            .create();

                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            message.setMessage("This message is removed ");
                            message.setFeeling(-1);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);




                          int lastMessageId = messages.size()-1;

                          int itemPosition = viewHolder.getAdapterPosition();
                          if (lastMessageId==itemPosition) {

                              HashMap<String, Object> lastMsgObj = new HashMap<>();

                              lastMsgObj.put("lastMsg", message.getMessage());
                              // lastMsgObj.put("lastMsgTime",date.getTime());

                              FirebaseDatabase.getInstance().getReference().child("chats")
                                      .child(senderRoom)
                                      .updateChildren(lastMsgObj);
                          }





                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(receverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);

                            if (lastMessageId==itemPosition) {
                                HashMap<String, Object> lastMsgObj1 = new HashMap<>();

                                lastMsgObj1.put("lastMsg", message.getMessage());
                                // lastMsgObj.put("lastMsgTime",date.getTime());

                                FirebaseDatabase.getInstance().getReference().child("chats")
                                        .child(receverRoom)
                                        .updateChildren(lastMsgObj1);
                            }



                            dialog.dismiss();
                        }
                    });




                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);

                            int lastMessageId = messages.size()-1;


                            int itemPosition = viewHolder.getAdapterPosition();

                            Message previousMessage = messages.get(itemPosition-1);

                            if (lastMessageId==itemPosition) {

                                HashMap<String, Object> lastMsgObj = new HashMap<>();

                                lastMsgObj.put("lastMsg",previousMessage.getMessage());
                                // lastMsgObj.put("lastMsgTime",date.getTime());

                                FirebaseDatabase.getInstance().getReference().child("chats")
                                        .child(senderRoom)
                                        .updateChildren(lastMsgObj);
                            }
                            dialog.dismiss();

                        }
                    });



                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });

            viewHolder.binding.txtMessage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                        popup.onTouch(view, motionEvent);
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

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //Log.d("TAG", "onLongClick: click");
                  //  Toast.makeText(context, "message", Toast.LENGTH_SHORT).show();
                    View view1 = LayoutInflater.from(context).inflate(R.layout.delete_mesage,null);
                    DeleteMesageBinding binding = DeleteMesageBinding.bind(view1);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(view1)
                            .create();




                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            FirebaseDatabase.getInstance().getReference()
                                    .child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);



                            int lastMessageId = messages.size()-1;

                            int itemPosition = viewHolder.getAdapterPosition();

                            Message previousMessage = messages.get(itemPosition-1);

                            if (lastMessageId==itemPosition) {

                                HashMap<String, Object> lastMsgObj = new HashMap<>();

                                lastMsgObj.put("lastMsg",previousMessage.getMessage());
                                // lastMsgObj.put("lastMsgTime",date.getTime());

                                FirebaseDatabase.getInstance().getReference().child("chats")
                                        .child(senderRoom)
                                        .updateChildren(lastMsgObj);
                            }
                            dialog.dismiss();

                        }
                    });



                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });

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
        IemSendBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = IemSendBinding.bind(itemView);

        }
    }

      public  class  ReceiverHolder extends  RecyclerView.ViewHolder
      {
          ItemReceiveBinding binding;

          public ReceiverHolder(@NonNull View itemView) {
              super(itemView);

               binding = ItemReceiveBinding.bind(itemView);
          }
      }
}
