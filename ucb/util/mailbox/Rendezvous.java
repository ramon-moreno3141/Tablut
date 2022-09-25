package ucb.util.mailbox;

import java.rmi.*;
import java.rmi.server.*;
import java.io.Serializable;
import java.util.*;


/** A Mailbox with a capacity of 0.  A depositer must wait until his message
 *  is received. */

public class Rendezvous<Msg extends Serializable>
	extends TypeCheckedMailbox<Msg>
{

	/** A new Rendezvous for messages of type MSGCLASS. */
	public static <Msg extends Serializable> 
	   Rendezvous<Msg> create (Class<Msg> msgClass) 
	{
		try {
			return new Rendezvous<Msg> (msgClass);
		} catch (RemoteException e) {
			throw new Error ("failed to create Rendezvous");
		}
	}

	/** A new Rendezvous for messages of unchecked type. */
	public static <Msg extends Serializable> 
	   Rendezvous<Msg> create () 
	{
		return create (null);
	}

	protected Rendezvous (Class<Msg> msgClass) throws RemoteException {
		super (msgClass);
		msg = null;
		open = true; 
	}

	/** Deposit MSG in this Mailbox.  If the Mailbox is full, wait
	 *  if necessary for space to be available.  MSG must be non-null.  
	 *  Throws Interrupted exception and does nothing if current thread 
	 *  is interrupted.  */
	public void deposit (Msg msg) throws InterruptedException, RemoteException
	{
		deposit (msg, -1);
	}

	/** Deposit MSG in this Mailbox, if this can be done within approximately
	 *  MILLIS milliseconds.  Returns true iff the message was deposited. 
	 *  Does not block if MILLIS is 0.  MSG must be non-null.  Throws 
	 *  Interrupted exception and does nothing if current thread 
	 *  is interrupted.  */
	public synchronized boolean deposit (Msg msg, long millis) 
		throws InterruptedException, RemoteException
	{
		if (msg == null)
			throw new IllegalArgumentException ("null message");
		long timeLeft;
		timeLeft = millis;
		while (open && this.msg != null && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		if (! open)
			throw new IllegalStateException ("mailbox closed");
		if (this.msg != null && timeLeft == 0)
			return false;
		forward (msg, millis);
		this.msg = msg;
		notifyAll ();
		return true;
	}

	/** Receive the next queued message in this Mailbox. Throws 
	 *  Interrupted exception and does nothing if current thread 
	 *  is interrupted.  */
	public Msg receive () throws InterruptedException {
		return receive (-1);
	}

	/** Receive the next queued message in this Mailbox, if one is
	 *  available within MILLIS milliseconds.  Returns null otherwise.  
	 *  Does not block if MILLIS is 0.  Throws Interrupted exception and 
	 *  does nothing if current thread is interrupted.  */
	public synchronized Msg receive (long millis) throws InterruptedException {
		long timeLeft;
		timeLeft = millis;
		while (open && msg == null && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		if (! open)
			throw new IllegalStateException ("mailbox closed");
		Msg result = msg;
		msg = null;
		notifyAll ();
		return result;
	}

	/** Forward copies of all messages (including any already present) to BOXES
	 *  in the order received.  Once forwarded to all destinations, messages 
	 *  are otherwise treated as usual.  Deposit blocks until all forwarding 
	 *  is complete.  */
	public synchronized void forwardTo (Mailbox<Msg> box) 
		throws InterruptedException, RemoteException 
	{
		forwardingBoxes.add (box);
	}

	public synchronized void forwardTo (List<Mailbox<Msg>> boxes) 
		throws InterruptedException, RemoteException 
	{
		forwardingBoxes.addAll (boxes);
	}    

	/** Stop forwarding copies of messages. */
	public synchronized void stopForwarding () {
		forwardingBoxes.clear ();
	}

	/** Wait for any queued message to be received, or MILLIS milliseconds,
	 *  whichever comes first.  Returns true if all pending messages were 
	 *  flushed within the time limit.  */
	public synchronized boolean flush (long millis) throws InterruptedException {
		long timeLeft;
		timeLeft = millis;
		while (open && msg != null && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		return ! open || msg == null;
	}
      
	/** Performs a flush(MILLIS) and then invalidates THIS for all future
	 *  use, deleting all remaining messages.  Any waiting threads or
	 *  subsequent calls receive an immediate InvalidStateException indicating
	 *  an invalid state.  In general, the thread that uses a Mailbox
	 *  to send messages is the one that closes it.  */
	public void close (long millis) throws InterruptedException {
		synchronized (this) {
			flush (millis);
			open = false;
			notifyAll ();
		}
		try {
			unexportObject (this, false);
		} catch (NoSuchObjectException e) {
		}
	}

	/** Short for close (0). */
	public void close () throws InterruptedException {
		close (0);
	}

	/** Wait for THIS to be closed.  Receives and throws away any messages
	 *  sent to THIS.  Throws InterruptedException if current
	 *  thread is interrupted first.  */
	public void awaitClose () throws InterruptedException {
		awaitClose (-1);
	}

	/** Wait for THIS to be closed, or MILLIS milliseconds, whichever comes
	 *  first.  Receives and throws away any messages sent to THIS.  
	 *  Returns true iff box is closed.
	 *  Throws InterruptedException if current thread is interrupted first.  */
	public synchronized boolean awaitClose (long millis) 
		throws InterruptedException 
	{
		long timeLeft;
		timeLeft = millis;
		while (open && timeLeft != 0) {
			if (msg != null) {
				msg = null;
				notifyAll ();
			}
			timeLeft = myWait (timeLeft);
		}
		return ! open;
	}

	/* Queries */

	/** True iff THIS is currently forwarding messages as a result of
	 *  forwardTo. */
	public synchronized boolean isForwarding ()  {
		return open && forwardingBoxes.size () > 0;
	}

	public boolean isClosed () {
		return ! open;
	}

	/** The capacity (see interface comment above) of THIS. */
	public int capacity () {
		return 0;
	}

	protected synchronized void forward (Msg msg, long millis) 
		throws RemoteException, InterruptedException 
	{
		if (forwardingBoxes.isEmpty ())
			return;
		RemoteException excp;
		excp = null;
		currentForwards.clear ();
		currentForwards.addAll (forwardingBoxes);
		for (Mailbox<Msg> box : currentForwards) {
			try {
				box.deposit (msg, millis);
			} catch (RemoteException e) {
				if (excp == null)
					excp = e;
			}
		}
		if (excp != null)
			throw excp;
	}

	protected long myWait (long limit) throws InterruptedException {
		if (limit == -1) {
			wait ();
			return -1;
		} else {
			long start = System.currentTimeMillis ();
			wait (limit);
			return Math.max (0, limit - System.currentTimeMillis () + start);
		}
	}

	private final ArrayList<Mailbox<Msg>> forwardingBoxes = 
		new ArrayList<Mailbox<Msg>> ();
	private final ArrayList<Mailbox<Msg>> currentForwards = 
		new ArrayList<Mailbox<Msg>> ();
	protected Msg msg;
	protected boolean open;
}

