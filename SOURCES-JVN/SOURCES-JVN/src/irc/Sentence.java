/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

public class Sentence implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
	String privateMessage;
  
	public Sentence() {
		data = "";
	}

	public Sentence(String priv) {
		data = "";
		privateMessage= priv;
	}

	public void setPrivateMessage(String msg)
	{
		privateMessage = msg;
	}

	public String getPrivateMessage()
	{
		return privateMessage;
	}


	
	public void write(String text) {
		System.out.println("This text :" + text + "is being saved!");
		data = text;
	}
	public String read() {
		return data;	
	}
	
}