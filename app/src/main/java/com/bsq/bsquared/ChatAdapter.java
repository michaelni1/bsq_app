package com.bsq.bsquared;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.ocpsoft.prettytime.PrettyTime;

public class ChatAdapter extends FirestoreRecyclerAdapter<Chat, ChatAdapter.ViewHolder> {
    PrettyTime p = new PrettyTime();
    String stored_username;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<Chat> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Chat model) {
        holder.message.setText(model.getMessage());

        if (model.getIs_host()) {
            String host_string = "Event host";
            holder.sender.setText(host_string);
            holder.sender.setTextColor(Color.parseColor("#C0C0C0"));
        }
        else if (stored_username.equals(model.getSender())) {
            holder.sender.setText(model.getSender());
            holder.sender.setTextColor(Color.parseColor("#CDCF64"));
        }
        else {
            holder.sender.setText(model.getSender());
            holder.sender.setTextColor(Color.parseColor("#000000"));
        }
        holder.timestamp.setText(p.format(model.getTimestamp()));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message, parent, false);

        return new ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView sender, message, timestamp;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            SharedPreferences get_pref = itemView.getContext().getSharedPreferences("username", Context.MODE_PRIVATE);
            stored_username = get_pref.getString("username", "");

            sender = itemView.findViewById(R.id.message_user);
            message = itemView.findViewById(R.id.message_text);
            timestamp = itemView.findViewById(R.id.message_time);

        }
    }
}
