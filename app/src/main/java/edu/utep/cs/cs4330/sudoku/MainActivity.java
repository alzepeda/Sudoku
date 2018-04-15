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
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
// ------------------------------------------//

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import edu.utep.cs.cs4330.sudoku.model.Board;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;

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

    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("1a9a8d20-3db7-11e8-b467-0ed5f89f718b");
    private static final int CONNECTION_TIMEOUT = 10000; // in milliseconds
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9
    };
    /** Width of number buttons automatically calculated from the screen size. */
    private static int buttonWidth;
    private Board board;
    private BoardView boardView;
    private WifiP2pManager mManager;
    private WifiManager wifiManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private ToggleButton bluetoothBTN, p2pBTN, wifiBTN;
    private NetworkAdapter network;
    private Socket socket;
    private ServerSocket serverSocket;
    private Socket incomingSocket;
    /** Handler associated with the UI thread. */
    private Handler handler;
    private boolean networkSwitch;
    private Button networkButton;
    private EditText alertInput;
    private String myIp;
    private String myPort;
    private AlertDialog networkDialog;
    private AlertDialog newGameApprovaleDialog;
    private AlertDialog inviteToNewGameDialog;
    private AlertDialog joinGameApprovalDialog;
    private AlertDialog waitForResponseAndPuzzleDialog;
    private String previousNetworkConnection;

    /**Buttons to represent levels of difficulty*/
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private String[] deviceNameArray;// show devices names
    private WifiP2pDevice[] deviceArray;// connect to a device
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
    /** All the number buttons. */
    private List<View> numberButtons;
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
    /**Below are all network operations*/

    private void startNetwork(){
        wifiBTN.setOnClickListener(x -> toggleNetwork());
        wifiBTN.setText("Turn On");
        wifiBTN.setBackgroundColor(Color.parseColor("#00ad06"));
        networkSwitch = false;
        newNetworkDialog();
    }

    private void alertDialogToConnect(){


    }
    private void sendMessage(){
        int[] networkBoardMessage = this.board.getNetworkBoardMessage();
        network.writeNew(this.boardSize,networkBoardMessage);
        inviteToNewGameDialog = new AlertDialog.Builder(MainActivity.this).create();
        inviteToNewGameDialog.setTitle("New Game");
        inviteToNewGameDialog.setMessage("Inviting to New Game...");
        inviteToNewGameDialog.show();

    }
    private void listerToPlayer(){
        new Thread(()->{
            try {
                serverSocket = new ServerSocket(8000);
                while(true){
                    incomingSocket = serverSocket.accept();
                    network = new NetworkAdapter(incomingSocket);
                    //joinShareGame();
                    setNetworkListener();
                    network.receiveMessagesAsync();

                    this.runOnUiThread(() -> closeNetworkDialog());

                    break;
                }
            }catch(Exception e){
                if(incomingSocket != null)
                    this.runOnUiThread(()->toast("Unknown error occurred while listening to port 8000\n\rError: " + e.toString()));
                try {
                    if(incomingSocket != null)
                        incomingSocket.close();
                    if(serverSocket != null)
                        serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                serverSocket = null;
                incomingSocket = null;
            }
        }).start();

    }
    private void connectToServer(String server, int port) {
        new Thread(() -> {
            socket = createSocket(server, port);
            if (socket != null) {
                network = new NetworkAdapter(socket);
                setNetworkListener();
                network.receiveMessagesAsync();
                network.writeJoin();

            }
            handler.post(() -> {
                toast(socket != null ? "Connected." : "Failed to connect!");
                if(socket == null) {
                    this.runOnUiThread(() -> {
                        waitForResponseAndPuzzleDialog.dismiss();
                        toggleNetwork();
                    });
                }

            });
        }).start();
    }

    private void setNetworkListener(){ /**========================NETWORK OPERATIONS========================================--*/
        network.setMessageListener(new NetworkAdapter.MessageListener() {
            @Override
            public void messageReceived(NetworkAdapter.MessageType type, int x, int y, int z, int[] others) {
                switch (type) {
                    case JOIN: runOnUiThread(() -> joinShareGame());break;
                    case JOIN_ACK: newSharedGame(y,others,false); runOnUiThread(() -> waitForResponseAndPuzzleDialog.dismiss());break;// x (response), y (size), others (board)
                    case NEW: newSharedGame(x,others,true); break;// x (size), others (board)
                    case NEW_ACK: newShareGameAck(x);break;// x (response)
                    case FILL: fillSharedSquare(x,y,z);break;// x (x), y (y), z (number)
                    case FILL_ACK: break;// x (x), y (y), z (number) DO NOTHING YET
                    case QUIT: runOnUiThread(() -> toggleNetwork()); break;
                }
            }
        });
    }


    private void joinShareGame(){ /**========================NETWORK OPERATIONS========================================--*/
        joinGameApprovalDialog = new AlertDialog.Builder(MainActivity.this).create();
        joinGameApprovalDialog.setTitle("Action Needed!");
        joinGameApprovalDialog.setMessage("Accept to join new network game request?");
        joinGameApprovalDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        declineJoinGame();
                        toggleNetwork();
                    }
                });
        joinGameApprovalDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        int[] networkBoardMessage = board.getNetworkBoardMessage();
                        network.writeJoinAck(boardSize,networkBoardMessage);
                        dialog.dismiss();
                    }
                });
        joinGameApprovalDialog.show();

    } /**========================NETWORK OPERATIONS========================================--*/
    private void disconnectFromNetwork(){
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (network != null) {
                network.writeQuit();
                network.close();
                network = null;
            }
            if (incomingSocket != null) {
                incomingSocket.close();
                incomingSocket = null;
            }
            if(serverSocket != null){
                serverSocket.close();
                serverSocket = null;
            }
        }catch(Exception e){ /**========================NETWORK OPERATIONS========================================--*/
            this.runOnUiThread(() -> toast(e.toString()));
        }

    } /**========================NETWORK OPERATIONS========================================--*/

    private void readyForNewGame(){
        network.writeNewAck(true);
    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void declineNewGame(){network.writeNewAck(false);}

    private void declineJoinGame(){network.writeJoinAck();}
    /**========================NETWORK OPERATIONS========================================--*/
    /** Create a socket to the given host and port number. */
    private Socket createSocket(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            return socket;
        } catch (Exception e) {
            Log.d("Sudoku", e.toString());
        }
        return null;
    }

    /**========================NETWORK OPERATIONS========================================--*/
    private void fillSharedSquare(int x, int y, int value){
        try {
            this.board.setSquareValue(x, y, value);
            this.board.setSquareByUser(x, y, true);
            this.board.getAllBoardPossibleNumbers();
            boardView.setSelectedCoordinates(-1, -1);
            boardView.setBoard(board);
            this.runOnUiThread(() -> this.boardView.invalidate());
            network.writeFillAck(x, y, value);
        }catch(Exception e){
            this.runOnUiThread(() -> toast(e.toString()));
        }
    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void newShareGameAck(int response){
        inviteToNewGameDialog.dismiss();
        if(response == 0)
            this.runOnUiThread(() -> toggleNetwork());
    } /**========================NETWORK OPERATIONS========================================--*/
    private void newSharedGame(int newSize,int[] sharedBoardConfig, boolean waitForApproval) {

        if (waitForApproval) {
            this.runOnUiThread(()-> getNewGameApproval(newSize, sharedBoardConfig));
        }
        else
            setupNewSharedGame(newSize,sharedBoardConfig);
    } /**========================NETWORK OPERATIONS========================================--*/
    private void setupNewSharedGame(int newSize,int[] sharedBoardConfig){
        boardSize = newSize;
        if (boardSize == 4) {
            this.normalPrefillNumbers = 4;
            this.hardPrefillNumbers = 2;
        } else {
            this.normalPrefillNumbers = 17;
            this.hardPrefillNumbers = 8;
        }
        board = new Board(boardSize, 0);
        board.updateBoardFromNetwork(sharedBoardConfig);
        boardView.setSelectedCoordinates(-1, -1);
        boardView.setBoard(board);
        this.boardView.invalidate();
    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void getNewGameApproval(int newSize,int[] sharedBoardConfig){
        newGameApprovaleDialog = new AlertDialog.Builder(MainActivity.this).create();
        newGameApprovaleDialog.setTitle("Action Needed!");
        newGameApprovaleDialog.setMessage("Accept a new game request?");
        newGameApprovaleDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        declineNewGame();
                        toggleNetwork();
                    }
                });
        newGameApprovaleDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setupNewSharedGame(newSize,sharedBoardConfig);
                        dialog.dismiss();
                        readyForNewGame();
                    }
                });
        newGameApprovaleDialog.show();
    }

    /** Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {

        if(!gameOver) {
            if (selectedX >= 0 && selectedY >= 0 && this.board.getSquare(selectedX,selectedY).isAvailable()) {
//
                if (n == 0 || this.board.isNumberAvailable(selectedX, selectedY, n)) {
//                    this.board.addCopyToUndoBoard(this.board.getSquareList());
//                    this.board.clearRedoBoard();
//                    this.board.setSquareValue(selectedX, selectedY, n);
//                    this.board.getSquare(selectedX, selectedY).setEnteredByUser(true);
//                    this.board.getAllBoardPossibleNumbers();
//                    this.boardView.setSelectedCoordinates(-1, -1);
//                    this.boardView.setBoard(this.board);
//                    this.boardView.invalidate();
//                    timer.restart();

                    /**========================NETWORK OPERATIONS========================================--*/
                    if (network != null && network.isSocket())
                        network.writeFill(selectedX, selectedY, n);
                } else
                    toast("Unable to enter number: " + n + " in: (" + selectedX + ", " + selectedY + ")");
            }
            /**===========================================================================================*/

            checkIfGameOver();
            if(!gameOver && !this.board.getSquare(selectedX,selectedY).isAvailable())
                toast("Unable to modify number: " + this.board.getSquare(selectedX,selectedY).getValue() + " in: (" + selectedX + ", " + selectedY + ")");
        }else
            toast("Game is over!");
    }



    public String getIPAddress(){
        new Thread(() -> {

            try {
                InetAddress localhost = Inet4Address.getLocalHost();
                myIp = localhost.getHostAddress().trim();
            } catch (Exception e) {
                System.out.println(e);
                myIp = "UNABLE_TO_DISPLAY_IP";
            }

        }).start();
        return myIp;
    }

    public String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }



}
