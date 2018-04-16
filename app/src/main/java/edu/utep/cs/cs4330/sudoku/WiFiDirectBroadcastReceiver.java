
/**
 * Created by Sebastian Gonzalez and Ana Zepeda.
 */
package edu.utep.cs.cs4330.sudoku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;



public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mainActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel mChannel, MainActivity mainActivity){

        this.mainActivity =mainActivity;
        this.mChannel = mChannel;
        this.mManager = manager;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                mainActivity.toast("Wifi ON");

            }
            else{
                mainActivity.toast("Wifi OFF");
            }

        }
        else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //Call requestPeers() to get a list of current peers
            if(mManager!=null){
                mManager.requestPeers(mChannel,mainActivity.peerListListener);

            }
        }
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            //REspond to new connection or disconnection
        }
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            //Respond to this devices wifi state change

        }
    }
}

