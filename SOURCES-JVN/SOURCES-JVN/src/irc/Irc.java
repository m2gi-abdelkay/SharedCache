/***
 * Irc class : simple implementation of a chat using JAVANAISE 
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	JvnObject       sentence;


  /**
  * main method
  * create a JVN object nammed IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
		   
		// initialize JVN
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		System.out.println("js is :" + js);
		// look up the IRC object in the JVN server
		// if not found, create it, and register it in the JVN server
		JvnObject jo = js.jvnLookupObject("IRC");
		   
		if (jo == null) {
			System.out.println("Creating object...");
			jo = js.jvnCreateObject((Serializable) new Sentence("Secret Message"));
			// after creation, I have a write lock on the object
			jo.jvnUnLock();
			js.jvnRegisterObject("IRC", jo);
		}
		// create the graphical part of the Chat application
		 new Irc(jo);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.getMessage());
	   }
	}

  /**
   * IRC Constructor
   @param jo the JVN object representing the Chat
   **/
	public Irc(JvnObject jo) throws JvnException {

		sentence = jo;
		System.out.println("Object received : " + jo);
		System.out.println(((Sentence)(sentence.jvnGetSharedObject())).getPrivateMessage());


		frame = new Frame("JVN - Distributed IRC");
		frame.setLayout(new BorderLayout(10, 10));
		frame.setBackground(new Color(240, 240, 240));

		// main text area
		text = new TextArea("", 10, 60, TextArea.SCROLLBARS_VERTICAL_ONLY);
		text.setEditable(false);
		text.setForeground(Color.red);
		text.setBackground(Color.black);
		text.setFont(new Font("Consolas", Font.PLAIN, 14));
		frame.add(text, BorderLayout.CENTER);
		
	
		// Input panel
		Panel inputPanel = new Panel(new BorderLayout(5, 5));
		data = new TextField(40);
		data.setFont(new Font("Consolas", Font.PLAIN, 14));
		inputPanel.add(data, BorderLayout.CENTER);

		// Buttons panel
		Panel buttonPanel = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
		Button read_button = new Button("Read");
		read_button.setBackground(new Color(200, 230, 255));
		read_button.setFont(new Font("Arial", Font.BOLD, 13));
		read_button.addActionListener(new readListener(this));

		Button write_button = new Button("Write");
		write_button.setBackground(new Color(220, 255, 220));
		write_button.setFont(new Font("Arial", Font.BOLD, 13));
		write_button.addActionListener(new writeListener(this));

		buttonPanel.add(read_button);
		buttonPanel.add(write_button);
		inputPanel.add(buttonPanel, BorderLayout.EAST);

		frame.add(inputPanel, BorderLayout.SOUTH);

		frame.setSize(600, 300);
		frame.setVisible(true);

	}

	
}


 /**
  * Internal class to manage user events (read) on the CHAT application
  **/
 class readListener implements ActionListener {
	Irc irc;
  
	public readListener (Irc i) {
		irc = i;
	}
   
 /**
  * Management of user events
  **/
	public void actionPerformed (ActionEvent e) {
	 try {
		// lock the object in read mode
		irc.sentence.jvnLockRead();
		
		// invoke the method
		String s = ((Sentence)(irc.sentence.jvnGetSharedObject())).read();
		System.out.println("s is : " + s);
		// unlock the object
		irc.sentence.jvnUnLock();
		
		// display the read value
		irc.data.setText(s);
		irc.text.append(s+"\n");
	   } catch (JvnException je) {
		   System.out.println("IRC problem : " + je.getMessage());
	   }
	}
}

 /**
  * Internal class to manage user events (write) on the CHAT application
  **/
 class writeListener implements ActionListener {
	Irc irc;
  
	public writeListener (Irc i) {
        	irc = i;
	}
  
  /**
    * Management of user events
   **/
	public void actionPerformed (ActionEvent e) {
	   try {	
		// get the value to be written from the buffer
    String s = irc.data.getText();
        	
    // lock the object in write mode
		irc.sentence.jvnLockWrite();
		System.out.println("I got the lock!");
		
		// invoke the method
		((Sentence)(irc.sentence.jvnGetSharedObject())).write(s);
		
		// unlock the object
		irc.sentence.jvnUnLock();

		// irc.data.setText("");
		// irc.text.append(s+"\n");
	 } catch (JvnException je) {
		   System.out.println("IRC problem  : " + je.getMessage());
	 }
	}
}



