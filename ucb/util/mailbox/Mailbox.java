package ucb.util.mailbox;

import java.io.Serializable;
import java.rmi.*;
import java.util.*;

/** A repository for messages, suitable for remote use.  Has facilities for
 *  forwarding copies of messages to other mailboxes.  Messages are received 
 *  and forwarded in the order deposited.  
 *  
 *  A mailbox has a "capacity", which is the maximum allowed excess 
 *  of completed deposits over completely processed messages.  A message is
 *  "completely" processed when it has been received (if retains() is true)
 *  and has been forwarded to all requesters.  A capacity of 
 *  Integer.MAX_VALUE indicates that there is no set limit.  When a Mailbox
 *  has a capacity of 0, it synchronizes senders and receivers: a deposit 
 *  does not complete until its message is picked up by receive (and 
 *  forwarded, if called for).
 */
public interface Mailbox<Msg extends Serializable> extends Remote {

	/** Deposit MSG in this Mailbox.  If the Mailbox is full, wait
	 *  if necessary for space to be available.  MSG must be non-null.  
	 *  Throws Interrupted exception and does nothing if current thread 
	 *  is interrupted.   Throws an IllegalStateException (or RemoteException)
	 *  if THIS is closed.  Throws IllegalArgumentException if MSG is null. */
	void deposit (Msg msg) throws RemoteException, InterruptedException;

	/** Deposit MSG in this Mailbox, if this can be done within approximately
	 *  MILLIS milliseconds.  Returns true iff the message was deposited. 
	 *  Does not block if MILLIS is 0.  MSG must be non-null.  Throws 
	 *  Interrupted exception and does nothing if current thread 
	 *  is interrupted.  Throws IllegalStateException (or RemoteException)
	 *  if THIS is closed. Throws IllegalArgumentException if MSG is null. */
	boolean deposit (Msg msg, long millis) 
		throws RemoteException, InterruptedException;

	/** Receive the next queued message in this Mailbox. Returns null
	 *  if THIS is closed.  Throws Interrupted exception and does nothing 
	 *  if the current thread is interrupted.  */
	Msg receive () throws RemoteException, InterruptedException;

	/** Receive the next queued message in this Mailbox, if one is
	 *  available within MILLIS milliseconds.  Returns null otherwise, 
	 *  including if THIS is closed.  Does not block if MILLIS is 0.  
	 *  Throws Interrupted exception and does nothing if current thread 
	 *  is interrupted.  */
	Msg receive (long millis) 
		throws RemoteException, InterruptedException;

	/** Forward copies of all messages (including any already present) to BOX
	 *  in the order received.  Once forwarded, messages are otherwise treated 
     *  as usual.  Deposit blocks until all forwarding is complete.  */
	void forwardTo (Mailbox<Msg> box) 
		throws RemoteException, InterruptedException;

	/** Forward copies of all messages (including any already present) to BOXES
	 *  in the order received.  Once forwarded to all destinations, messages 
	 *  are otherwise treated as usual.  Deposit blocks until all forwarding 
	 *  is complete.  */
	void forwardTo (List<Mailbox<Msg>> boxes)
		throws RemoteException, InterruptedException;

	/** Stop forwarding copies of messages. */
	void stopForwarding () throws RemoteException;

	/** Wait for all queued messages to be received, or MILLIS milliseconds,
	 *  whichever comes first.  During this wait, the Mailbox acts as if
	 *  full from the point of view of all threads attempting to deposit.
	 *  Returns true if all messages were flushed within the time limit.  */
	boolean flush (long millis) throws RemoteException, InterruptedException;

	/** Performs a flush(MILLIS) and then invalidates THIS for all future
	 *  use, deleting all remaining messages.  Any waiting threads or
	 *  subsequent calls receive an immediate RemoteException indicating
	 *  an invalid state.  In general, the thread that uses a Mailbox
	 *  to send messages is the one that closes it.  */
	void close (long millis) throws RemoteException, InterruptedException;

	/** Short for close (0). */
	void close () throws RemoteException, InterruptedException;

	/* Queries */

	/** True iff THIS is currently forwarding messages as a result of
	 *  forwardTo. */
	boolean isForwarding () throws RemoteException;

	/** True iff THIS is closed. */
	boolean isClosed () throws RemoteException;

	/** The capacity (see interface comment above) of THIS. */
	int capacity () throws RemoteException;

	/** The (runtime) type of message sent through this box. */
	Class<Msg> messageType () throws RemoteException;

	/** Returns THIS, assuming that THIS conveys messages of class MSGCLASS. */
	<T extends Serializable> 
	    Mailbox<T> checkType (Class<T> msgClass) throws RemoteException;

}
