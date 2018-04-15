package edu.utep.cs.cs4330.sudoku;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class exampleOfNetwork {


    private Board board;
    private TextView timerView;

    private BoardView boardView;
    private NetworkAdapter network;
    private Socket socket;
    private ServerSocket serverSocket;
    private Socket incomingSocket;
    /** Handler associated with the UI thread. */
    private Handler handler;
    private static final int CONNECTION_TIMEOUT = 15000; // in milliseconds

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        newGame();


        setupNetworkInit();
    } /**========================NETWORK OPERATIONS========================================--*/

    private void setupNetworkInit(){
        networkButton = findViewById(R.id.networkButton);
        networkButton.setOnClickListener(x -> toggleNetwork());
        networkButton.setText("Turn On");
        networkButton.setBackgroundColor(Color.parseColor("#00ad06"));
        networkSwitch = false;
        newNetworkDialog();

    } /**========================NETWORK OPERATIONS========================================--*/

    private void newNetworkDialog(){
        alertInput = new EditText(this);
        alertInput.setText(previousNetworkConnection);
        networkDialog = new AlertDialog.Builder(MainActivity.this).create();
        myIp = this.getIPAddress(true);
        myPort = "8000";
        String myConnection = myIp + ":" + myPort;
        networkDialog.setTitle("Connection");
        networkDialog.setMessage("Host Information: " + myConnection + "\r\n\r\nEnter the IP Followed by ':' and port number to connect to a Server");
        networkDialog.setView(alertInput);
        networkDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Disconnect",
                (dialog, which) -> {
                    dialog.dismiss();
                    toggleNetwork();
                    if(incomingSocket == null && serverSocket != null)
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }); /**========================NETWORK OPERATIONS========================================--*/
        networkDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Connect",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] serverInfo;
                        serverInfo = alertInput.getText().toString().split(":");
                        if(alertInput.getText().toString().contains(":") && serverInfo.length == 2) {
                            try {
                                connectToServer(serverInfo[0], Integer.parseInt(serverInfo[1]));
                                waitForJoinAckResponse();
                                previousNetworkConnection = alertInput.getText().toString();
                            }catch(Exception e){
                                toast("Unable to Connect to:" + alertInput.getText().toString());
                                toggleNetwork();
                            }

                        }else {
                            toast("Unable to Connect to:" + alertInput.getText().toString());
                            toggleNetwork();
                        }
                        dialog.dismiss();
                        if(incomingSocket == null && serverSocket != null)
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                });
    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void waitForJoinAckResponse(){
        waitForResponseAndPuzzleDialog = new AlertDialog.Builder(MainActivity.this).create();
        waitForResponseAndPuzzleDialog.setTitle("Joining Game");
        waitForResponseAndPuzzleDialog.setMessage("Waiting for Response and puzzle...");
        waitForResponseAndPuzzleDialog.show();
    }

    private void toggleNetwork(){
        /**========================NETWORK OPERATIONS========================================--*/
        if(networkSwitch) {
            networkButton.setText("Turn On");
            networkButton.setBackgroundColor(Color.parseColor("#00ad06"));
            networkSwitch = false;
            disconnectFromNetwork();
        }else {
            newNetworkDialog();
            networkDialog.show();
            networkButton.setText("Turn Off");
            networkButton.setBackgroundColor(Color.parseColor("#c90404"));
            networkSwitch = true;
            listenForClient();
        }

    }

    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {
        // WRITE YOUR CODE HERE ...
        //
        //toast("New clicked.");
        newGame(); /**========================NETWORK OPERATIONS========================================--*/
        if(network != null && network.isSocket())
            sendNetworkMessage();
        this.boardView.invalidate();
    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void sendNetworkMessage(){

        int[] networkBoardMessage = this.board.getNetworkBoardMessage();
        network.writeNew(this.boardSize,networkBoardMessage);
        inviteToNewGameDialog = new AlertDialog.Builder(MainActivity.this).create();
        inviteToNewGameDialog.setTitle("New Game");
        inviteToNewGameDialog.setMessage("Inviting to New Game...");
        inviteToNewGameDialog.show();

    }
    /**========================NETWORK OPERATIONS========================================--*/
    private void listenForClient(){
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
    } /**========================NETWORK OPERATIONS========================================--*/
    private void closeNetworkDialog(){this.networkDialog.dismiss();}
    /**========================NETWORK OPERATIONS========================================--*/
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

                if (n == 0 || this.board.isNumberAvailable(selectedX, selectedY, n)) {
                    this.board.addCopyToUndoBoard(this.board.getSquareList());
                    this.board.clearRedoBoard();
                    this.board.setSquareValue(selectedX, selectedY, n);
                    this.board.getSquare(selectedX, selectedY).setEnteredByUser(true);
                    this.board.getAllBoardPossibleNumbers();
                    this.boardView.setSelectedCoordinates(-1, -1);
                    this.boardView.setBoard(this.board);
                    this.boardView.invalidate();
                    timer.restart();

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
