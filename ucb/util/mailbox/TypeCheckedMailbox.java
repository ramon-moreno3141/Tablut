package ucb.util.mailbox;

import java.rmi.*;
import java.rmi.server.*;
import java.io.Serializable;

/** A Mailbox that carries the dynamic runtime type (type Class) of its 
 *  messages. */
abstract class TypeCheckedMailbox<Msg extends Serializable> 
	extends UnicastRemoteObject implements Mailbox<Msg> 
{

	TypeCheckedMailbox (Class<Msg> msgClass) throws RemoteException {
		this.msgClass = msgClass;
	}

	public Class<Msg> messageType () {
		return msgClass;
	}

	@SuppressWarnings ("unchecked")
	public <T extends Serializable> Mailbox<T> checkType (Class<T> msgClass) {
		if (this.msgClass == null || this.msgClass.equals (msgClass))
			return (Mailbox<T>) this;
		else
			throw new ClassCastException ("Mailbox cannot convey messages of type " + msgClass);
	}

	private Class<Msg> msgClass;
}

