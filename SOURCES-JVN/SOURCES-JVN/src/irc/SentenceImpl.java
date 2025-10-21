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
	// version helps detect updates and ordering
	private int version = 0;
  
	public SentenceImpl() {
		data = "";
	}

	@Override
	@WriteLock
	public void write(String text) {
		data = text;
		version++;
		System.out.println("[SentenceImpl] write: v=" + version + " text='" + text + "' (id=" + System.identityHashCode(this) + ")");
	}

	@Override
	@ReadLock
	public String read() {
		System.out.println("[SentenceImpl] read: v=" + version + " (id=" + System.identityHashCode(this) + ")");
		return data;    
	}
	
}