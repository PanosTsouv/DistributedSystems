package com.example.mymusicstreamingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class OfflineListAdapter extends RecyclerView.Adapter<OfflineListAdapter.FileNameViewHolder> {

    private ArrayList<String> mData;
    private Context mContext;
    private ListItemClickListener mOnListItemListener;

    OfflineListAdapter(Context context, ArrayList<String> data, ListItemClickListener onListItemListener) {
        mContext = context;
        mData = data;
        mOnListItemListener = onListItemListener;
    }

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex);
    }

    @NonNull
    @Override
    public FileNameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.offline_list_item, parent, false);
        return new FileNameViewHolder(view, mOnListItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FileNameViewHolder holder, int position) {
        String song = mData.get(position);
        holder.bind(song);
    }

    /**
     * Returns the number of items to display.
     */
    @Override
    public int getItemCount() {
        return mData.size();
    }

    String getItem(int id) {
        return mData.get(id);
    }

    static class FileNameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView fileNameView;

        ListItemClickListener mOnListItemListener;

        FileNameViewHolder(@NonNull View itemView, ListItemClickListener onListItemListener) {
            super(itemView);
            fileNameView = itemView.findViewById(R.id.file_name_string);
            mOnListItemListener = onListItemListener;
            itemView.setOnClickListener(this);
        }

        void bind(String name) {
            fileNameView.setText(name);
        }

        @Override
        public void onClick(View v) {
            mOnListItemListener.onListItemClick(getAdapterPosition());
        }
    }
}
