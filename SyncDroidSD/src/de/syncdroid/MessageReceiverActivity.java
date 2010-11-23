package de.syncdroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import de.syncdroid.service.HandlerAdapter;
import de.syncdroid.service.MessageHandler;
import de.syncdroid.service.SyncService;

public abstract class MessageReceiverActivity extends AbstractActivity implements MessageHandler {
	static final String TAG = "MessageReceiverActivity";

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new HandlerAdapter(this));
	
	/** Messenger for communicating with service. */
	protected Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	private boolean mIsBound;
	/**
	 * Class for interacting with the main interface of the service.
	 */
	protected ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
//	        mCallbackText.setText("Attached.");
	        Log.d(TAG, "Attached");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null,
	                    SyncService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

//	            // Give it some value as an example.
//	            msg = Message.obtain(null,
//	                    SyncService.MSG_SET_VALUE, this.hashCode(), 0);
//	            mService.sen(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        //mCallbackText.setText("Disconnected.");

	    }
	};
	
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();

        try {
            Message msg = Message.obtain(null,
                    SyncService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
        unbindService(mConnection);
	}

	/*
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
	    bindService(new Intent(LocationEditActivity.this, 
	            SyncService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	   // mCallbackText.setText("Binding.");
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                		SyncService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        //mCallbackText.setText("Unbinding.");
	    }
	}
	*/

}
