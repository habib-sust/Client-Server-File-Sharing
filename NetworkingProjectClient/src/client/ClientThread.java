/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 *
 * @author Habibur
 */
public class ClientThread implements Runnable{
    
      Socket socket;
    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    MainForm main;
    StringTokenizer stringTokenizer;
    protected DecimalFormat decimalFormat = new DecimalFormat("##,#00");
    
    public ClientThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[IOException]: "+ e.getMessage(), "Error", Color.RED, Color.RED);
        }
    }

    @Override
    public void run() {
        try {
            while(!Thread.currentThread().isInterrupted()){
                String data = dataInputStream.readUTF();
                stringTokenizer = new StringTokenizer(data);
                /** Get Message CMD **/
                String CMD = stringTokenizer.nextToken();
                switch(CMD){
                    case "CMD_MESSAGE":
                      //  SoundEffect.MessageReceive.play(); //  Play Audio clip
                        String msg = "";
                        String frm = stringTokenizer.nextToken();
                        while(stringTokenizer.hasMoreTokens()){
                            msg = msg +" "+ stringTokenizer.nextToken();
                        }
                        main.appendMessage(msg, frm, Color.MAGENTA, Color.BLUE);
                        break;
                        
                    case "CMD_ONLINE":
                        Vector online = new Vector();
                        while(stringTokenizer.hasMoreTokens()){
                            String list = stringTokenizer.nextToken();
                            if(!list.equalsIgnoreCase(main.username)){
                                online.add(list);
                            }
                        }
                        main.appendOnlineList(online);
                        break;
                    
                        
                    //  This will inform the client that there's a file receive, Accept or Reject the file  
                    case "CMD_FILE_XD":  // Format:  CMD_FILE_XD [sender] [receiver] [filename]
                        String sender = stringTokenizer.nextToken();
                        String receiver = stringTokenizer.nextToken();
                        String fname = stringTokenizer.nextToken();
                        int confirm = JOptionPane.showConfirmDialog(main, "From: "+sender+"\nFilename: "+fname+"\nwould you like to Accept.?");
                        //SoundEffect.FileSharing.play(); //   Play Audio
                        if(confirm == 0){ // client accepted the request, then inform the sender to send the file now
                            /* Select were to save this file   */
                            main.openFolder();
                            try {
                                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                // Format:  CMD_SEND_FILE_ACCEPT [ToSender] [Message]
                                String format = "CMD_SEND_FILE_ACCEPT "+sender+" accepted";
                                dataOutputStream.writeUTF(format);
                                
                                /*  this will create a filesharing socket to handle incoming file and this socket will automatically closed when it's done.  */
                                Socket fSoc = new Socket(main.getMyHost(), main.getMyPort());
                                DataOutputStream fdos = new DataOutputStream(fSoc.getOutputStream());
                                fdos.writeUTF("CMD_SHARINGSOCKET "+ main.getMyUsername());
                                /*  Run Thread for this   */
                                new Thread(new ReceivingFileThread(fSoc, main)).start();
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        } else { // client rejected the request, then send back result to sender
                            try {
                                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                // Format:  CMD_SEND_FILE_ERROR [ToSender] [Message]
                                String format = "CMD_SEND_FILE_ERROR "+sender+" Client rejected your request or connection was lost.!";
                                dataOutputStream.writeUTF(format);
                            } catch (IOException e) {
                                System.out.println("[CMD_FILE_XD]: "+e.getMessage());
                            }
                        }                       
                        break;   
                        
                    default: 
                        main.appendMessage("[CMDException]: Unknown Command "+ CMD, "CMDException", Color.RED, Color.RED);
                    break;
                }
            }
        } catch(IOException e){
            main.appendMessage(" Server Connection was lost, please try again later.!", "Error", Color.RED, Color.RED);
        }
    }
}
