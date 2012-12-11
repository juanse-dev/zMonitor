package com.example.zzmonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.achartengine.GraphicalView;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ZMonitor extends Activity implements OnCheckedChangeListener {
	
	private static final String TAG = "ZMonitor";

	//Atributos para graficar
	private static GraphicalView view;
	private LineGraph line = new LineGraph();
	private static Thread thread;
	private Handler myHandler = new Handler();
	
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

	private static final String ARCHIVO = "monitoreoZMonitor.txt";
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    //punto obtenido de BT
    private int puntoX=-1;
    private int puntoY=-1;
    private String tempPunto = "";
    private boolean inicioX=false;
    private boolean inicioY=false;
    private boolean inicioPareja=false;
    
    private CheckBox cbTriggerMode;
    private boolean triggerMode = false;
    
    
    
    // The handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                	Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                	ZMonitor.this.sendMessage("OK");
                	myHandler.removeCallbacks(graficarTask);
                	myHandler.postDelayed(graficarTask, 10);
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                	Toast.makeText(getApplicationContext(), "Connecting... ", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	Toast.makeText(getApplicationContext(), "No connected! ", Toast.LENGTH_SHORT).show();
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //Toast.makeText(getApplicationContext(), "Send message: "+ writeMessage, Toast.LENGTH_LONG).show();
               // mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
            	String readMessage = (String) msg.obj;
                if (msg.arg1 > 0) {
                	ZMonitor.this.sendMessage(readMessage);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    if(line.isEmpty()){Toast.makeText(getApplicationContext(), "Receiving data", Toast.LENGTH_SHORT).show();}
                    ZMonitor.this.readMessage(readMessage);
                }
                /*byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                                
                ZMonitor.this.sendMessage(readMessage);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                if(line.isEmpty()){Toast.makeText(getApplicationContext(), "Receiving data", Toast.LENGTH_SHORT).show();}
                ZMonitor.this.readMessage(readMessage);*/
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_zmonitor);
		
		cbTriggerMode = (CheckBox) findViewById(R.id.cbTrigger);
		cbTriggerMode.setOnCheckedChangeListener(this);
		

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
		//graficar();
		
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
		myHandler.removeCallbacks(graficarTask);
		
	}
	
	
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
		if(isChecked){
			triggerMode = true;
		}
		else{
			triggerMode = false;
		}
	}

	protected void readMessage(String readMessage) {
		
		Log.d("ZMonitor","readMessage: "+ readMessage);
		for(int i = 0; i < readMessage.length(); i++){
			if(!inicioPareja && (readMessage.charAt(i) == '(') ){
				//Log.d(TAG, "Comienzo de punto '(' en pos " + i);
				inicioPareja = true;
				tempPunto = "";
			}
			else if(inicioPareja){
				if(!(readMessage.charAt(i) ==')')){
					//Log.d(TAG, "Almacenando char " + readMessage.charAt(i) + " en pos " + i);
					tempPunto = tempPunto + readMessage.charAt(i);
				}
				else{
					String[] pareja = tempPunto.split(",");
					//puntoX = Integer.parseInt(pareja[0]);
					if(line.isEmpty()){
						puntoX = Integer.parseInt(pareja[0]);
					}
					else {
						puntoX = Integer.parseInt(pareja[0]) + (int)(line.getX(line.getItemCount()-1));						
					}
					puntoY = Integer.parseInt(pareja[1]);
					inicioPareja = false;
					//Toast.makeText(getApplicationContext(), "Received message: (" + puntoX+","+puntoY+")", Toast.LENGTH_LONG).show();
					nuevoPunto(puntoX, puntoY);
					//guardarEnMemoria(puntoX, puntoY);
					puntoX=-1;
					puntoY=-1;
				}
			}
			
		}
	}

	private void guardarEnMemoria(int puntoX2, int puntoY2) {
		//---guardar en memoria
		
		try {
		    File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite()){
		        File f = new File(root.getAbsoluteFile(), ARCHIVO);
		        FileWriter gpxwriter = new FileWriter(f,true);
		        BufferedWriter out = new BufferedWriter(gpxwriter);
		        
		        //out.write("zZMonitor.\n Datos Obtenidos: \n\n");
		        
		        
		        String linea=puntoX2 + ", "+puntoY2+"\n";	
		        out.write(linea);	
		        out.flush();		        
		        
		        out.close();
		    }
		} catch (IOException e) {
		    Log.e("none", "Could not write file " + e.getMessage());
		}
		
		//------Fin guardar en memoria
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		 if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChart();
        }
		
		//view = line.getView(this);
		//setContentView(view);
	}
	
	private void setupChart() {

        // Initialize the BluetoothChatService to perform BT connections
        mChatService = new BluetoothChatService(this, mHandler);
		
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
		view = line.getView(this);
		 layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
         
	
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
       
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        case R.id.clean:
            // Ensure this device is discoverable by others
        	reiniciarActivity(this);
            return true;
        case R.id.disconnect:
            // Ensure this device is discoverable by others
            sendMessage("END");
        	if (mChatService != null) mChatService.stop();
            return true;
        
        }
        return false;
    }
    
  //reinicia una Activity
    public static void reiniciarActivity(Activity actividad){
            Intent intent=new Intent();
            intent.setClass(actividad, actividad.getClass());
            //llamamos a la actividad
            actividad.startActivity(intent);
            //finalizamos la actividad actual
            actividad.finish();
    }

	@Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop the Bluetooth chat services
        if (mChatService != null){
        	sendMessage("END");
        	mChatService.stop();
        }
        
    }
    
    private Runnable graficarTask = new Runnable() {
		public void run() {
			guardarEnMemoria(puntoX, puntoY);
		}
    
    };
     public void nuevoPunto(int x,int y)
     {
    	 Point p = new Point((int)x, (int)y);
    	 
    	 if(line.getItemCount() >= 60){
    		 if(!triggerMode){
    			 line.pop();
    		 } else {
    			 line.pop();
    			 boolean rising = false; // Comienzo de gráfica con pendiente positiva
    			 boolean trigger = false; // Comienzo de onda en cierto nivel (ej 512)
    			 
    			 while(!rising || !trigger){
    				 if(line.isEmpty()){
    					 break;
    				 }
    				 if(line.getItemCount() > 1){
    					 if(line.getY(0)<line.getY(1)){
    						 rising = true;
    					 }else{
    						 rising = false;
    					 }
    					 if(rising){
    						 if(Math.abs(line.getY(0)-512)<1){
    							 trigger = true;
    							 break;
    						 } else {
    							 trigger = false;
    						 }
    					 }
    				 }
    				 line.pop();
    			 }
    		 }
    	 }
    	 line.addNewPoints(p);
    	 view.repaint();
    	 
     }
    
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChart();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
     super.onConfigurationChanged(newConfig);
     setContentView(R.layout.activity_zmonitor);
    }
}
