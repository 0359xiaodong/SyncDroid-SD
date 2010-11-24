package de.syncdroid;

import java.util.ArrayList;

import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class MessageService extends GuiceService implements MessageHandler {
	private static final String TAG = "MessageService";

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1001;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 1002;

    final Messenger mMessenger = new Messenger(new HandlerAdapter(this));
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    

    abstract public void handleRegisterClient();
    abstract public void handleUnregisterClient();
    

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    public void sendMessageToClients(int what, Object obj) {
        for (int i = mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null,
                		what, obj));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
            	Log.w(TAG, "removing client: " + i);
                mClients.remove(i);
            }
        }
    }
    
    @Override
    public boolean handleMessage(Message msg) {
    	Log.d(TAG, "msg.what: " + msg.what);
        switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                handleRegisterClient();
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                handleUnregisterClient();
                break;
        }
        
        return true;
    }

}
