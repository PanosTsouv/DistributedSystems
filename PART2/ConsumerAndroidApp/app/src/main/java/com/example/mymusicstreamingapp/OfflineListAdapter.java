package com.example.mymusicstreamingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class OfflineListAdapter extends RecyclerView.Adapter<OfflineListAdapter.FileNameViewHolder> {

    private ArrayList<File> mData;
    private Context mContext;
    private ListItemClickListener mOnListItemListener;

    OfflineListAdapter(Context context, ArrayList<File> data, ListItemClickListener onListItemListener) {
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
        holder.bind(mData.get(position).getName(),new DecimalFormat("#.##")
                .format(mData.get(position).length()/1024.0/1024.0) + " MB");
    }

    /**
     * Returns the number of items to display.
     */
    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class FileNameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView fileNameView;
        TextView fileLengthView;
        ListItemClickListener mOnListItemListener;

        FileNameViewHolder(@NonNull View itemView, ListItemClickListener onListItemListener) {
            super(itemView);
            fileNameView = itemView.findViewById(R.id.file_name_string);
            fileLengthView = itemView.findViewById(R.id.file_length_string);
            mOnListItemListener = onListItemListener;
            itemView.setOnClickListener(this);
        }

        void bind(String name, String length) {
            fileNameView.setText(name);
            fileLengthView.setText(length);
        }

        @Override
        public void onClick(View v) {
            mOnListItemListener.onListItemClick(getAdapterPosition());
        }
    }
}
