package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//------for p2p, wifi Direct ------- //
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
// ------------------------------------------//

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import edu.utep.cs.cs4330.sudoku.model.Board;

/**
 * HW1 template for developing an app to play simple Sudoku games.
 * You need to write code for three callback methods:
 * newClicked(), numberClicked(int) and squareSelected(int,int).
 * Feel free to improved the given UI or design your own.
 *
 * <p>
 *  This template uses Java 8 notations. Enable Java 8 for your project
 *  by adding the following two lines to build.gradle (Module: app).
 * </p>
 *
 * <pre>
 *  compileOptions {
 *  sourceCompatibility JavaVersion.VERSION_1_8
 *  targetCompatibility JavaVersion.VERSION_1_8
 *  }
 * </pre>
 *
 * @author Yoonsik Cheon
 */
public class MainActivity extends AppCompatActivity {

    private Board board;
    private BoardView boardView;
    private WifiP2pManager mManager;
    private WifiManager wifiManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private ToggleButton bluetoothBTN, p2pBTN, wifiBTN;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private String[] deviceNameArray;// show devices names
    private WifiP2pDevice[] deviceArray;// connect to a device

    /**Buttons to represent levels of difficulty*/

    /** All the number buttons. */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9
    };

    /** Width of number buttons automatically calculated from the screen size. */
    private static int buttonWidth;

    private int x=-1, y=-1;

    private BluetoothAdapter adapter;
    private BluetoothDevice peer;
    private NetworkAdapter netAd;
    private BluetoothServerSocket server;
    private BluetoothSocket client;
    private List<BluetoothDevice> listDevices;
    private ArrayList<String> nameDevices;
    private int temp;
    private PrintStream logger;
    private OutputStream outSt;
    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("1a9a8d20-3db7-11e8-b467-0ed5f89f718b");
    private NetworkAdapter connection;
    private NetworkAdapter.MessageListener heyListen;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mymenu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothBTN = findViewById(R.id.blueTbtn);
        wifiBTN = findViewById(R.id.wifiBTN);
        p2pBTN = findViewById(R.id.p2pBTN);

        wifiBTN.setOnClickListener(view -> toggleWifi());
        bluetoothBTN.setOnClickListener(view -> toggleBluetooth());
        p2pBTN.setOnClickListener(view -> toggleP2P());
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager,mChannel,this);


        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        board = new Board();
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);
        board.setGrid();

        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            final int number = i; // 0 for delete button
            View button = findViewById(numberIds[i]);
            button.setOnClickListener(e -> numberClicked(number));
            numberButtons.add(button);
            setButtonWidth(button);
            button.setEnabled(false);
        }

        listDevices = new ArrayList<BluetoothDevice>();
        nameDevices = new ArrayList<String>();
        peer = null;
        adapter = BluetoothAdapter.getDefaultAdapter();
        outSt = new ByteArrayOutputStream(1024);
        logger = new PrintStream(outSt);

        heyListen = new NetworkAdapter.MessageListener() {
            @Override
            public void messageReceived(NetworkAdapter.MessageType type, int x, int y, int z, int[] others) {
                switch (type.header){
                    case "join:":
                        break;
                    case "join_ack:":
                        break;
                    case "new:":
                        break;
                    case "new_ack:":
                        break;
                    case "fill:":
                        Log.d("Progress", "Progress");
                        board.setNumber(x,y,z);
                        break;
                    case "fill_ack:":
                        break;
                    case "quit:":
                        break;
                }
            }
        };
    }

    private void toggleP2P() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                toast("discovery success");
                Intent intentActivity = new Intent(MainActivity.this, P2PListActivity.class);
                startActivity(intentActivity);

            }

            @Override
            public void onFailure(int i) {
                toast("discovery failure");

            }
        });



    }
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            if(!peersList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peersList.getDeviceList());

                deviceNameArray = new String[peersList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peersList.getDeviceList().size()];
                int index = 0;
                for(WifiP2pDevice device : peersList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                P2PListActivity.listView.setAdapter(adapter);
            }
            if(peers.size() == 0){
                toast("No devices found :(");
            }

        }
    };

    private void toggleBluetooth() {
        toast("Bluetooth");

    }

    private void toggleWifi() {
        netAd.startCommunications();
            if (wifiManager.isWifiEnabled()) {
                toast("turning WIFI off");
                wifiManager.setWifiEnabled(false);
            } else {
                toast("turning WIFI on");
                wifiManager.setWifiEnabled(true);
            }
        }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.easyMenu:
                board.setLevel(1);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.mediumMenu:
                board.setLevel(2);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.hardMenu:
                board.setLevel(3);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.action4x4menu:
                board.setSize(4);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                toast("Size changed to "+String.valueOf(board.getSize()));
                return true;
            case R.id.action9x9menu:
                board.setSize(9);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                toast("Size changed to "+String.valueOf(board.getSize()));
                return true;
        }
        return true;
    }

    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {
        board.setGrid();
        boardView.noneSelected();
        boardView.postInvalidate();
        for(int i = 0; i < board.getGrid().length; i++){
            numberButtons.get(i).setEnabled(false);
        }
        toast("New game started");

    }

    public void solveClicked(View view){
        Solver.solveSudoku(board.getGrid());
        boardView.postInvalidate();
    }

    /** Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {
        toast("Number clicked: " + n);
        board.setNumber(x, y, n);
        boardView.postInvalidate();
        if(board.complete()){
            Toast.makeText(this, "Congratulations! The puzzle is complete!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback to be invoked when a square is selected in the board view.
     *
     * @param x 0-based column index of the selected square.
     * @param x 0-based row index of the selected square.
     */
    private void squareSelected(int x, int y) {
        toast(String.format("Square selected: (%d, %d)", x, y));
        boolean[] possible = board.possible(x,y);
        this.x = x;
        this.y = y;
        if(board.original(x,y)){
            for(int i = 0; i < possible.length; i++){
                numberButtons.get(i).setEnabled(false);
            }
            boardView.postInvalidate();
            return;
        }
        for(int i = 0; i < possible.length; i++){
            numberButtons.get(i).setEnabled(possible[i]);
        }
        if(board.getSize() == 4){
            for(int i = 5; i<10; i++){
                numberButtons.get(i).setEnabled(false);
            }
        }
        boardView.postInvalidate();
    }


    /** Show a toast message. */
    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Set the width of the given button calculated from the screen size. */
    private void setButtonWidth(View view) {
        if (buttonWidth == 0) {
            final int distance = 2;
            int screen = getResources().getDisplayMetrics().widthPixels;
            buttonWidth = (screen - ((9 + 1) * distance)) / 9; // 9 (1-9)  buttons in a row
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = buttonWidth;
        view.setLayoutParams(params);
    }

    /**
     * Callback to be invoked when the solveable button is clicked
     */
    public void solvableClicked(View view) {
        if (Solver.solveable(board.getGrid())) {
            Toast.makeText(this, "The board is solveable", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "The board is not solveable", Toast.LENGTH_LONG).show();
        }
    }

    /** Callback to be invoked when the help button is clicked */
    public void helpClicked(View view) {
        if(boardView.help == false){
            boardView.help = true;
        }else{
            boardView.help = false;
        }
        boardView.postInvalidate();
    }

    //Client Functions
    public void onClient(View v){
        if (!adapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : adapter.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : adapter.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
        }
    }


    public void ConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        peer = device;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {
            Log.e("error_socket", "Socket: " + tmp.toString() + " create() failed", e);
        }

        client = tmp;
        Log.d("socket", peer.toString());
    }

    public void runClient() {
        // Cancel discovery because it otherwise slows down the connection.
        adapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            client.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                client.close();
            } catch (IOException closeException) {
                Log.e("Close socket", "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.

        toast("Connected");
        if(client == null){
            toast("Null client");
        }else {
            connection = new NetworkAdapter(client, logger);
            connection.setMessageListener(heyListen);
            connection.receiveMessagesAsync();
        }
    }

    public void off(View v){
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void clientClickedApp(View view) {
        //utilities.clientClicked(view);
        onClient(view);
        // setup the alert builder
        if (listDevices.isEmpty()) {
            Intent turnOn = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(turnOn, 0);
            onClient(view);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paired Devices");

        String[] arrDevices = nameDevices.toArray(new String[nameDevices.size()]);
        int checkedItem = 0;
        builder.setSingleChoiceItems(arrDevices, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toast(arrDevices[which]);
                temp = which;
            }
        });

        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                peer = listDevices.get(temp);
                Log.d("devices", peer.getAddress());
                ConnectThread(peer);
                runClient();
            }
        });
        builder.setNeutralButton("PAIR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent turnOn = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(turnOn, 0);
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**P2p methods*/
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}
