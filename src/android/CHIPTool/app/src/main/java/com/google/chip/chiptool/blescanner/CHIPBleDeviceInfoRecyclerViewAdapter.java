package com.google.chip.chiptool.blescanner;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.chip.chiptool.R;
import com.google.chip.chiptool.blescanner.dummy.CHIPBleDeviceContainer.CHIPBleDeviceInfo;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link CHIPBleDeviceInfo}. TODO: Replace the
 * implementation with code for your data type.
 */
public class CHIPBleDeviceInfoRecyclerViewAdapter extends
    RecyclerView.Adapter<CHIPBleDeviceInfoRecyclerViewAdapter.ViewHolder> {

  private final List<CHIPBleDeviceInfo> mValues;

  public CHIPBleDeviceInfoRecyclerViewAdapter(List<CHIPBleDeviceInfo> items) {
    mValues = items;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_chip_ble_device_info, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    holder.mItem = mValues.get(position);
    holder.mIdView.setText(mValues.get(position).id);
    holder.mContentView.setText(mValues.get(position).content);
  }

  @Override
  public int getItemCount() {
    return mValues.size();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {

    public final View mView;
    public final TextView mIdView;
    public final TextView mContentView;
    public CHIPBleDeviceInfo mItem;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mIdView = (TextView) view.findViewById(R.id.item_number);
      mContentView = (TextView) view.findViewById(R.id.content);
    }

    @Override
    public String toString() {
      return super.toString() + " '" + mContentView.getText() + "'";
    }
  }
}