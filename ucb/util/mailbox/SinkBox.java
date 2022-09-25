package ucb.util.mailbox;

import java.rmi.*;
import java.rmi.server.*;
import java.io.Serializable;
import java.util.*;


/** A Mailbox that throws away all mail (after any forwarding). */

public class SinkBox<Msg extends Serializable> extends QueuedMailbox<Msg> {

	/** A new SinkBox for messages of type MSGCLASS. */
	public static <Msg extends Serializable> 
        SinkBox<Msg> create (Class<Msg> msgClass)
	{
		try {
			return new SinkBox<Msg> (msgClass);
		} catch (RemoteException e) {
			throw new Error ("failed to create SinkBox");
		}
	}

	/** A new SinkBox with no type checking. */
	public static <Msg extends Serializable> SinkBox<Msg> create ()
	{
		return create (null);
	}

	protected SinkBox (Class<Msg> msgClass) throws RemoteException {
		super (msgClass, 1);
	}

	protected void enqueue (Msg msg) {
	}

}

