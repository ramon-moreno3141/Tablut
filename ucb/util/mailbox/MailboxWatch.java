package ucb.util.mailbox;

import java.io.*;
import java.rmi.*;

/** A MailboxWatch takes a stream of forwarded messages from a Mailbox and
 *  prints them to a specified stream.  It is a thread and can be stopped
 *  by being interrupted. */
public class MailboxWatch<Msg extends Serializable> extends Thread {
	/** A new MailboxWatch on BOX, reporting to STR.  ID is a string used to
	 *  label each reported message. */
	public MailboxWatch (Class<Msg> msgClass, Mailbox<Msg> box, 
						 String id, PrintStream str) {
		try {
			myBox = new QueuedMailbox<Msg> (msgClass, 10);
			box.forwardTo (myBox);
			this.str = str;
			this.id = id;
			start ();
		} catch (Exception e) {
			System.err.println ("error watching Mailbox (" + id + ")");
			System.exit (1);
		} 
	}

	public void run () {
		try {
			while (true) {
				Msg msg0 = myBox.receive ();
				if (msg0 == null)
					return;
				str.println (id + msg0);
				str.flush ();
			}
		} catch (RemoteException e) {
			System.err.printf ("error watching Mailbox (%s): %s%n", id, e);
		} catch (InterruptedException e) {
		}
	}

	public void close () {
		try {
			this.interrupt ();
		} catch (RuntimeException e) {
		}
	}

	private Mailbox<Msg> myBox;
	private String id;
	private PrintStream str;
}
