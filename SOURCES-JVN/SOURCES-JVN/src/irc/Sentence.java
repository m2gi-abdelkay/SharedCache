/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface ReadWriteAnnotation {
	String[] readMethods() default {};
	String[] writeMethods() default {};
}

@ReadWriteAnnotation(readMethods = {"read"}, writeMethods = {"write"})
public class Sentence implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String 	data;
  
	public Sentence() {
		data = "";
	}


	
	public void write(String text) {
		//System.out.println("This text :" + text + "is being saved!");
		data = text;
	}
	public String read() {
		return data;	
	}
	
}