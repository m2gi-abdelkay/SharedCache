/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

import jvn.Annotations.*;
import jvn.Utils.Sentence;

public class SentenceImpl implements java.io.Serializable, Sentence {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public SentenceImpl() {
		data = "";
	}

	@Override
	@WriteLock
	public void write(String text) {
		System.out.println("This text :" + text + " is being saved!");
		data = text;
	}

	@Override
	@ReadLock
	public String read() {
		return data;	
	}
	
}