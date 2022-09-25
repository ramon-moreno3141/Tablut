package ucb.util.mailbox;

import java.rmi.*;
import java.rmi.server.*;
import java.io.Serializable;
import java.util.*;


/** A Mailbox with a specified (positive) capacity */

public class QueuedMailbox<Msg extends Serializable>
	extends TypeCheckedMailbox<Msg> 
{

	/** A new QueuedMailbox<MSGCLASS> with capacity () fixed at CAPACITY. */
	public static <Msg extends Serializable> 
	    QueuedMailbox<Msg> create (Class<Msg> msgClass, int capacity) {
		try {
			return new QueuedMailbox<Msg> (msgClass, capacity);
		} catch (RemoteException e) {
			throw new Error ("failed to create QueuedMailbox");
		}
	}

	/** A new QueuedMailbox with capacity () fixed at CAPACITY, with
	 *  no type checking. */
	public static <Msg extends Serializable> 
        QueuedMailbox<Msg> create (int capacity) 
	{
		return create (null, capacity);
	}

	/** A new QueuedMailbox with given CAPACITY carrying messages of type
	 *  MSGCLASS.  The factory method create, rather than this constructor, 
	 *  is intended for public use. */
	protected QueuedMailbox (Class<Msg> msgClass, int capacity) 
		throws RemoteException 
	{
		super (msgClass);
		if (capacity < 1)
			throw new IllegalArgumentException ("invalid capacity: " + capacity);
		this.capacity = capacity;
		open = true; depositBlocks = 0;
	}

	/** Deposit MSG in this Mailbox.  If the Mailbox is full, wait
	 *  if necessary for space to be available.  MSG must be non-null.  
	 *  Throws Interrupted exception and does nothing if current thread 
	 *  is interrupted.   Throws an IllegalStateException (or RemoteException)
	 *  if THIS is closed.  Throws IllegalArgumentException if MSG is null. */
	public void deposit (Msg msg) throws InterruptedException, RemoteException
	{
		deposit (msg, -1);
	}

	/** Deposit MSG in this Mailbox, if this can be done within approximately
	 *  MILLIS milliseconds.  Returns true iff the message was deposited. 
	 *  Does not block if MILLIS is 0.  MSG must be non-null.  Throws 
	 *  Interrupted exception and does nothing if current thread 
	 *  is interrupted.  Throws IllegalStateException (or RemoteException)
	 *  if THIS is closed. Throws IllegalArgumentException if MSG is null. */
	public synchronized boolean deposit (Msg msg, long millis) 
		throws InterruptedException, RemoteException
	{
		if (msg == null)
			throw new IllegalArgumentException ("null message");
		long timeLeft;
		timeLeft = millis;
		while (open && depositBlocks > 0 && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		if (! open)
			throw new IllegalStateException ("mailbox closed");
		if (depositBlocks > 0 && timeLeft == 0)
			return false;
		forward (msg, millis);
		enqueue (msg);
		if (queue.size () >= capacity)
			depositBlocks += 1;
		notifyAll ();
		return true;
	}

	/** Receive the next queued message in this Mailbox. Returns null
	 *  if THIS is closed.  Throws Interrupted exception and does nothing 
	 *  if the current thread is interrupted.  */
	public Msg receive () throws InterruptedException {
		return receive (-1);
	}

	/** Receive the next queued message in this Mailbox, if one is
	 *  available within MILLIS milliseconds.  Returns null otherwise, 
	 *  including if THIS is closed.  Does not block if MILLIS is 0.  
	 *  Throws Interrupted exception and does nothing if current thread 
	 *  is interrupted.  */
	public synchronized Msg receive (long millis) throws InterruptedException {
		long timeLeft;
		timeLeft = millis;
		while (open && queue.size () == 0 && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		if (! open || queue.size () == 0)
			return null;
		if (queue.size () == capacity) 
			depositBlocks -= 1;
		notifyAll ();
		return queue.remove (0);
	}

	/** Forward copies of all messages (including any already present) to BOXES
	 *  in the order received.  Once forwarded to all destinations, messages 
	 *  are otherwise treated as usual.  Deposit blocks until all forwarding 
	 *  is complete.  */
	public synchronized void forwardTo (Mailbox<Msg> box) 
		throws InterruptedException, RemoteException 
	{
		if (box != this)
			forwardingBoxes.add (box);
		for (Msg m : new ArrayList<Msg> (queue)) 
			forward (m, -1);
	}

	/** Forward copies of all messages (including any already present) to BOXES
	 *  in the order received.  Once forwarded to all destinations, messages 
	 *  are otherwise treated as usual.  Deposit blocks until all forwarding 
	 *  is complete.  */
	public synchronized void forwardTo (List<Mailbox<Msg>> boxes) 
		throws InterruptedException, RemoteException 
	{
		forwardingBoxes.addAll (boxes);
		forwardingBoxes.remove (this);
		for (Msg m : new ArrayList<Msg> (queue)) 
			forward (m, -1);
	}    

	/** Stop forwarding copies of messages. */
	public synchronized void stopForwarding () {
		forwardingBoxes.clear ();
	}

	/** Wait for all queued messages to be received, or MILLIS milliseconds,
	 *  whichever comes first.  During this wait, the Mailbox acts as if
	 *  full from the point of view of all threads attempting to deposit.
	 *  Returns true if all messages were flushed within the time limit.  */
	public synchronized boolean flush (long millis) throws InterruptedException {
		long timeLeft;
		timeLeft = millis;
		while (open && depositBlocks > 0 && timeLeft != 0)
			timeLeft = myWait (timeLeft);
		if (! open)
			return true;
		if (timeLeft == 0 && queue.size () > 0)
			return false;
		try {
			depositBlocks += 1;
			while (open && queue.size () > 0 && timeLeft != 0)
				timeLeft = myWait (timeLeft);
			if (! open)
				return true;
			if (timeLeft == 0 && queue.size () > 0)
				return false;
			return true;
		} finally {
			depositBlocks -= 1;
			notifyAll ();
		}
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
			unexportObject (this, true);
		} catch (NoSuchObjectException e) {
		}
	}

	/** Short for close (0). */
	public void close () throws InterruptedException {
		close (0);
	}

	/* Queries */

	/** True iff THIS is currently forwarding messages as a result of
	 *  forwardTo. */
	public synchronized boolean isForwarding ()  {
		return open && forwardingBoxes.size () > 0;
	}

	/** True iff THIS is closed. */
	public boolean isClosed () {
		return ! open;
	}

	/** The capacity (see interface comment above) of THIS. */
	public int capacity () {
		return capacity;
	}

	/** Place MSG at the end of the message queue. */
	protected void enqueue (Msg msg) {
		queue.add (msg);
	}

	/** Perform forwarding of MSG to all recipients, waiting for up to 
	 *  MILLIS milliseconds for each deposit to be accepted. */
	protected synchronized void forward (Msg msg, long millis) 
		throws RemoteException, InterruptedException 
	{
		if (forwardingBoxes.isEmpty ())
			return;
		RemoteException excp;
		excp = null;
		try {
			depositBlocks += 1;
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
		} finally {
			depositBlocks -= 1;
			notifyAll ();
		}
	}

	/** Wait for up to LIMIT milliseconds or the next notify or interrupt.
	 *  Returns the remaining time. */
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
	protected final LinkedList<Msg> queue = new LinkedList<Msg> ();
	protected int capacity;
	protected boolean open;
	protected int depositBlocks;
}

