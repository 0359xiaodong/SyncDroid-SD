package de.syncdroid.transfer.impl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arturh
 * Date: 28.11.10
 * Time: 20:27
 * To change this template use File | Settings | File Templates.
 */
public class ScpFileTransferClient extends AbstractFileTransferClient {
    private String hostname;
    private String username;
    private String password;

    private Context context;

    private FTPClient ftpClient;

    private JSch jsch;
    private Session session;
    private ChannelExec channel;

    public ScpFileTransferClient(String hostname, String username, String password, Context context) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.context = context;
    }

    public boolean connect() {
        try {
            Log.i(TAG, "scp connect to '" + username + "@" + hostname + "'");
			// connect to ftp server
            jsch = new JSch();
            session = jsch.getSession(username, hostname, 22);

            try {
                session.connect();

                if(session.isConnected() == false) {
                    Log.e(TAG, "scp connect failed !!");
                }
            } catch(JSchException e) {
                Log.e(TAG, "scp connect failed", e);
                return false;
            }

                channel = (ChannelExec) session.openChannel("exec");
            UserInfo ui = new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            channel.disconnect();
            session.disconnect();
        } catch (JSchException e) {
            Log.e(TAG, "scp connect failed", e);
        }
        return true;
    }

    public boolean transfer(File file, String targetPath) {
        Log.e(TAG, "scp transfer of '" + targetPath + "'");
        try{
            jsch=new JSch();
            session = jsch.getSession(username, hostname, 22);
            channel = (ChannelExec) session.openChannel("exec");

            UserInfo ui = new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            // exec 'scp -t rfile' remotely
            String command = "scp -p -t "+ targetPath;

            Log.i(TAG, "setCommand(" + command + ")");
            channel.setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if(checkAck(in)!=0){
                Log.e(TAG, "scp ack failed 1");
                return false;
            }
            Log.i(TAG, "ack 1");

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = file.length();
            command="C0644 " + filesize + " " + targetPath + "\n";
            Log.i(TAG, "writing command: '" + command + "'");
            out.write(command.getBytes()); out.flush();

            if(checkAck(in) != 0){
                Log.e(TAG, "scp ack failed 2");
                return false;
            }
            Log.i(TAG, "ack 2");

            // send a content of lfile
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[1024];

            while(true){
                int len = fis.read(buf, 0, buf.length);
                if(len <= 0) break;
                out.write(buf, 0, len); //out.flush();
            }

            fis.close();
            fis=null;
            // send '\0'
            buf[0]=0; out.write(buf, 0, 1); out.flush();
            if(checkAck(in)!=0){
                Log.e(TAG, "scp ack failed 3");
                return false;
            }
            Log.i(TAG, "ack 3");
            out.close();

            channel.disconnect();
            session.disconnect();

            fis.close();
            return true;
        } catch(Exception e) {
            Log.e(TAG, "scp transfer failed for '" + targetPath + "'", e);
            return false;
        }
    }

    public boolean disconnect() {
        return true;
    }

    static int checkAck(InputStream in) throws IOException{
      int b=in.read();
      // b may be 0 for success,
      //          1 for error,
      //          2 for fatal error,
      //          -1
      if(b==0) return b;
      if(b==-1) return b;

      if(b==1 || b==2){
        StringBuffer sb=new StringBuffer();
        int c;
        do {
      c=in.read();
      sb.append((char)c);
        }
        while(c!='\n');
        if(b==1){ // error
      System.out.print(sb.toString());
        }
        if(b==2){ // fatal error
      System.out.print(sb.toString());
        }
      }
      return b;
    }

    @Override
    protected boolean mkdir(String name) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected boolean cwd(String name) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public class MyUserInfo implements UserInfo, UIKeyboardInteractive{
        public String getPassword() { return ""; }
        public String getPassphrase() { return null; }
        public boolean promptPassphrase(String message){ return true; }

        private Boolean result;

        private void setResult(boolean result) {
            this.result = result;
        }

        public boolean promptYesNo(String s) {
            result = null;

            /*
            AlertDialog.Builder adb=new AlertDialog.Builder(context);
            adb.setMessage(s);
            adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setResult(true);
                }
            });
            adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setResult(false);
                }
            });
            adb.show();

            while(result == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }             */

            return true;

            //return result;
        }

        public boolean promptPassword(String message){
            Log.d(TAG, "promptPassword(" + message + ")");
            return false;
        }

        public void showMessage(String message){
            Log.d(TAG, "showMessage(" + message + ")");
            /*
            AlertDialog.Builder adb=new AlertDialog.Builder(context);
            adb.setMessage(message);
            adb.show();*/
        }

        public String[] promptKeyboardInteractive(String destination,
                String name,
                String instruction,
                String[] prompt,
                boolean[] echo){

            Log.d(TAG, "promptKeyboardInteractive()");

            List<String> responses = new ArrayList<String>();
            for(int i=0; i<prompt.length; i++){
                responses.add(password);
            }

            return responses.toArray(new String[]{});
        }
    }
}
