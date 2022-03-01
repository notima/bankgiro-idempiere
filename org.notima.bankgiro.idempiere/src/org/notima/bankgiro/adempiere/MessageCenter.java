package org.notima.bankgiro.adempiere;

import java.awt.Component;
import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Show information messages. If environment is graphical, the JOptionPane is used.
 * With non-graphical environment messages are logged. 
 * If confirmation is required the default answer is used and logged.
 * 
 * Messages are looked up using the ADempiere messages mechanism. This means that
 * the messages are defined in the message table. If the message isn't defined
 * the raw message is displayed.
 * 
 * @author daniel.tamm
 *
 */
public class MessageCenter {

	private static boolean headless = GraphicsEnvironment.isHeadless();
	
	private static CLogger		log = CLogger.getCLogger(MessageCenter.class);

	/**
	 * Ask the user to confirm the question.
	 *  
	 * @param question
	 * @param defaultAnswer
	 * @return
	 */
	public static boolean confirm(String question, boolean defaultAnswer) {
		if (!headless) {
			return JOptionPane.showConfirmDialog(null, Msg.getMsg(Env.getCtx(), question))==JOptionPane.OK_OPTION;
		} else {
			log.info(question + " : Defaulting to " + (defaultAnswer ? "Y" : "N"));
			return defaultAnswer;
		}
	}

	/**
	 * Shows information message
	 * 
	 * @param message
	 */
	public static void info(String message) {
		if (!headless) {
	        JOptionPane.showMessageDialog(null, Msg.getMsg(Env.getCtx(),message));
		} else {
			log.info(Msg.getMsg(Env.getCtx(), message));
		}
	}
	
	/**
	 * Shows information message
	 * 
	 * @param c			The graphical component to "anchor" this message to.
	 * @param message
	 */
	public static void info(Component c, String message) {
		if (!headless) {
	        JOptionPane.showMessageDialog(c, Msg.getMsg(Env.getCtx(),message));
		} else {
			log.info(Msg.getMsg(Env.getCtx(), message));
		}
	}

	/**
	 * Shows information message
	 * 
	 * @param c			The graphical component to "anchor" this message to.
	 * @param title		Title of message box
	 * @param message	Message
	 */
	public static void info(Component c, String title, String message) {
		
		if (!headless) {
        JOptionPane.showMessageDialog(c,
                Msg.getMsg(Env.getCtx(),message),
                Msg.getMsg(Env.getCtx(),title),
                JOptionPane.INFORMATION_MESSAGE);
		} else {
			log.info(Msg.getMsg(Env.getCtx(),title) + " : " + Msg.getMsg(Env.getCtx(),message));
		}
		
	}
	
	/**
	 * Shows error message
	 * 
	 * @param c			The graphical component to "anchor" this message to.
	 * @param title		Title of message box
	 * @param message	Message
	 */
	public static void error(Component c, String title, String message) {
		
		if (!headless) {
        JOptionPane.showMessageDialog(c,
                Msg.getMsg(Env.getCtx(),message),
                Msg.getMsg(Env.getCtx(),title),
                JOptionPane.ERROR_MESSAGE);
		} else {
			log.severe(Msg.getMsg(Env.getCtx(),title) + " : " + Msg.getMsg(Env.getCtx(),message));
		}
		
	}
	
	/**
	 * Shows error message
	 * 
	 * @param c			The graphical component to "anchor" this message to.
	 * @param title		Title of message box
	 * @param message	Message
	 */
	public static void error(String message) {
		
		if (!headless) {
        JOptionPane.showMessageDialog(null,
                Msg.getMsg(Env.getCtx(),message),
                null,
                JOptionPane.ERROR_MESSAGE);
		} else {
			log.severe(Msg.getMsg(Env.getCtx(),message));
		}
		
	}
	
	
	
}
