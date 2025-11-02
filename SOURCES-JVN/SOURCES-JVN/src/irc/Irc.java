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
import jvn.Handler.JvnHandler;
import jvn.Server.JvnServerImpl;
import jvn.Utils.JvnException;
import jvn.Utils.JvnObject;
import jvn.Utils.Sentence;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	JvnObject       sentence;
	Sentence        sentenceProxy;  // Add proxy field
	JvnServerImpl   server;  // Store server reference
	Thread          shutdownHook;  // Store shutdown hook reference so we can remove it


  /**
  * main method
  * create a JVN object nammed IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
		   // Determine object name from args (default "IRC")
		   String objName = "IRC";
		   if (argv != null && argv.length > 0 && argv[0] != null && argv[0].length() > 0) {
			objName = argv[0];
		   }

		// initialize JVN
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		// look up the object in the JVN server
		// if not found, create it, and register it in the JVN server
		JvnObject jo = js.jvnLookupObject(objName);
		   
		if (jo == null) {
			System.out.println("[IRC] Creating object...");
				jo = js.jvnCreateObject((Serializable) new SentenceImpl());
				// after creation, I have a write lock on the object
				jo.jvnUnLock();
				js.jvnRegisterObject(objName, jo);
		}
		// create the graphical part of the Chat application
		 new Irc(jo, js);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.getMessage());
	   }
	}

  /**
   * IRC Constructor
   @param jo the JVN object representing the Chat
   @param js the JVN server instance
   **/
	public Irc(JvnObject jo, JvnServerImpl js) throws JvnException {

		sentence = jo;
		server = js;
		// Create proxy for automatic locking
		sentenceProxy = (Sentence) JvnHandler.newInstance(jo);
		System.out.println("Object received : " + jo);
		
		// Add shutdown hook for CTRL-C (simulated crash)
		shutdownHook = new Thread(() -> {
			System.out.println("[IRC] Shutdown hook triggered (CTRL-C or kill signal)");
			try {
				server.jvnTerminate();
				System.out.println("[IRC] Server terminated successfully");
			} catch (Exception e) {
				System.err.println("[IRC] Error during shutdown: " + e.getMessage());
			}
		});
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		frame=new Frame();
		frame.setLayout(new BorderLayout(10,10));
		
		// Add window listener for GUI close
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("[IRC] Window closing, terminating server...");
				try {
					// Remove shutdown hook to prevent double termination
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
					server.jvnTerminate();
					System.out.println("[IRC] Server terminated successfully");
				} catch (Exception ex) {
					System.err.println("[IRC] Error during termination: " + ex.getMessage());
				}
				System.exit(0);
			}
		});
		
		text=new TextArea(10,60);
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
	@Override
	public void actionPerformed (ActionEvent e) {
	 try {
		// Use proxy - locking is handled automatically
		String s = irc.sentenceProxy.read();
		System.out.println("s is :" + s);
		
		// display the read value
		//irc.data.setText(s);
		irc.text.append(s+"\n");
	   } catch (Exception ex) {
		   System.out.println("IRC problem : " + ex.getMessage());
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
	@Override
	public void actionPerformed (ActionEvent e) {
	   try {	
		// get the value to be written from the buffer
    String s = irc.data.getText();
	System.out.println("Got text : " + s);
        	
    // Use proxy - locking is handled automatically
		irc.sentenceProxy.write(s);
		System.out.println("Write completed!");
		
	 } catch (Exception ex) {
		   System.out.println("IRC problem  : " + ex.getMessage());
	 }
	}
}



