package de.syncdroid.service;

import android.os.Handler;
import android.os.Message;

public class HandlerAdapter extends Handler {
	private MessageHandler messageHandler;
	
	public HandlerAdapter(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}
	@Override
	public void handleMessage(Message msg) {
		if(messageHandler.handleMessage(msg) == false) {
			super.handleMessage(msg);
		}
	}
}
