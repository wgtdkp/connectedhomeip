package com.google.chip.chiptool.commissioner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.google.chip.chiptool.R;
import java.util.Arrays;
import java.util.Vector;

public class NetworkAdapter extends BaseAdapter {
  /*
  private static  final NetworkInfo[] MOCK_NETWORKS = {
          new NetworkInfo(
                  new BorderAgentInfo("bed room",
                          new byte[] {0xde},
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
          new NetworkInfo(
                  new BorderAgentInfo("living room",
                          "0xdeadface",
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
          new NetworkInfo(
                  new BorderAgentInfo("bath room",
                          "0xdeadface",
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
          new NetworkInfo(
                  new BorderAgentInfo("second floor",
                          "0xdeadface",
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
          new NetworkInfo(
                  new BorderAgentInfo("third floor",
                          "0xdeadface",
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
          new NetworkInfo(
                  new BorderAgentInfo("forth floor",
                          "0xdeadface",
                          "127.0.0.1",
                          49191,
                          "3aa55f91ca47d1e4e71a08cb35e91591"
                  )),
  };
  */

  private Vector<NetworkInfo> networks;

  private LayoutInflater inflater;

  NetworkAdapter(Context context) {
    inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    networks = new Vector<>();
  }

  public boolean addNetwork(NetworkInfo newNetwork) {
    for (NetworkInfo network : networks) {
      if (network.getNetworkName().equals(newNetwork.getNetworkName()) &&
              Arrays.equals(network.getExtendedPanId(), newNetwork.getExtendedPanId())) {
        network.merge(newNetwork);
        return false;
      }
    }

    networks.add(newNetwork);
    notifyDataSetChanged();
    return true;
  }

  @Override
  public int getCount() {
    return networks.size();
  }

  @Override
  public Object getItem(int position) {
    return networks.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup container) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.commissioner_network_list_item, container, false);
    }
    TextView networkNameText = convertView.findViewById(R.id.network_name);
    networkNameText.setText(networks.get(position).getNetworkName());
    return convertView;
  }
}
