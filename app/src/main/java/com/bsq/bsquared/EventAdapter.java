package com.bsq.bsquared;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.ocpsoft.prettytime.PrettyTime;

public class EventAdapter extends FirestoreRecyclerAdapter<Event, EventAdapter.ViewHolder> {
    PrettyTime p = new PrettyTime();

    public EventAdapter(@NonNull FirestoreRecyclerOptions<Event> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull final EventAdapter.ViewHolder holder, int position, @NonNull final Event model) {
        holder.desc.setText(model.getDesc());
        holder.age_group.setText(model.getageGroup());
        holder.timestamp.setText(p.format(model.getTimestamp()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(), JoinEvent.class);
                intent.putExtra("loc", model.getLoc());
                intent.putExtra("event_id", model.getEvent_id());
                intent.putExtra("desc", model.getDesc());

                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @NonNull
    @Override
    public EventAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_post, parent, false);

        return new EventAdapter.ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView desc, age_group, timestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            desc = itemView.findViewById(R.id.event_desc);
            age_group = itemView.findViewById(R.id.event_age_group);
            timestamp = itemView.findViewById(R.id.event_time);
        }
    }
}
