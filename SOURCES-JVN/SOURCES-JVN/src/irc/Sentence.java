/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

import jvn.Annotations.*;

public class Sentence implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public Sentence() {
		data = "";
	}

	@WriteLock
	public void write(String text) {
		//System.out.println("This text :" + text + "is being saved!");
		data = text;
	}
	@ReadLock
	public String read() {
		return data;	
	}
	
}