package ucb.util;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

import java.util.ArrayList;

/** A kind of registry that provides a very simple means of exchanging remote
 *  objects by name.  One can create a registry, allowing the constructor
 *  to select an available port.  One can find a bound object by name over
 *  a sequence of port numbers and hosts.
 *  @author P. N. Hilfinger */
public class SimpleObjectRegistry {

    /** The default values for LOWPORT and HIGHPORT arguments. */
    public static final int
        DEFAULT_PORTS = 31,
        DEFAULT_LOW_PORT = Registry.REGISTRY_PORT,
        DEFAULT_HIGH_PORT = DEFAULT_LOW_PORT + DEFAULT_PORTS - 1;

    /** Return a remote object named NAME in some registry on HOST on
     *  some port number between LOWPORT and HIGHPORT.  Throws
     *  NotBoundException if no such object is found for whatever reason
     *  (i.e., either because no registry is found or because NAME
     *  is unbound on all such registries.) */
    public static Remote findObject(String name, String host,
                                     int lowPort, int highPort)
        throws NotBoundException {
        boolean aRegistryFound;
        aRegistryFound = false;
        for (int rport = lowPort; rport <= highPort; rport += 1) {
            try {
                return LocateRegistry.getRegistry(host, rport).lookup(name);
            } catch (NotBoundException e) {
                aRegistryFound = true;
            } catch (RemoteException e) {
                /* Ignore RemoteException. */
            }
        }
        if (aRegistryFound) {
            throw new NotBoundException("name not bound in any registry on "
                                        + host + ": " + name);
        } else {
            String msg =
                String.format("no registries found on %s, ports %d .. %d",
                              host, lowPort, highPort);
            throw new NotBoundException(msg);
        }
    }

    /** Return findObject(NAME, HOST, DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT).
     */
    public static Remote findObject(String name, String host)
        throws NotBoundException {
        return findObject(name, host, DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT);
    }

    /**  Return a remote object named NAME in some registry on one of the
     *   hosts in HOSTS on some port number between LOWPORT and HIGHPORT.
     *   Throws NotBoundException if no such object is found for
     *   whatever reason (i.e., either because no registry is found or
     *   because NAME is unbound on all such registries.) */
    public static Remote findObject(String name, String[] hosts,
                                    int lowPort, int highPort)
        throws NotBoundException {
        boolean aRegistryFound;
        aRegistryFound = false;
        for (int i = 0; i < hosts.length; i += 1) {
            try {
                return findObject(name, hosts[i], lowPort, highPort);
            } catch (NotBoundException e) {
                if (!e.getMessage().startsWith("name not bound")) {
                    aRegistryFound = true;
                }
            }
        }
        if (aRegistryFound) {
            throw new NotBoundException("name not bound on any host: " + name);
        } else {
            throw new NotBoundException("registry not found on any host");
        }
    }

    /** Return findObject(NAME, HOSTS, DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT).
     */
    public static Remote findObject(String name, String[] hosts)
        throws NotBoundException {
        return findObject(name, hosts, DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT);
    }

    /** Create a new SimpleObjectRegistry on one of the ports LOWPORT to
     *  HIGHPORT, if possible.  Throws RemoteException if no registry can be
     *  created on any of these ports. */
    public SimpleObjectRegistry(int lowPort, int highPort)
        throws RemoteException {
        _registry = null;
        for (int p : _freePorts) {
            if (p >= lowPort && p <= highPort) {
                _freePorts.remove((Object) p);
                try {
                    _registry = LocateRegistry.getRegistry(p);
                    _port = p;
                    return;
                } catch (RemoteException e) {
                    /* Ignore RemoteException */
                }
            }
        }
        for (_port = lowPort; _port <= highPort; _port += 1) {
            try {
                _registry = LocateRegistry.createRegistry(_port);
                return;
            } catch (RemoteException e) {
                /* Ignore RemoteException */
            }
        }
        throw new RemoteException("No port available");
    }

    /** Return SimpleObjectRegistry(PORT, PORT). */
    public SimpleObjectRegistry(int port) throws RemoteException {
        this(port, port);
    }

    /** Same as SimpleObjectRegistry(DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT). */
    public SimpleObjectRegistry() throws RemoteException {
        this(DEFAULT_LOW_PORT, DEFAULT_HIGH_PORT);
    }

    /** Return the port number on which this registry is exported. */
    public int port() {
        return _port;
    }

    /** Bind NAME to VALUE in this registry, replacing any existing binding. */
    public synchronized void rebind(String name, Remote value) {
        if (_registry == null) {
            throw new IllegalStateException("registry not active");
        }
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        try {
            _registry.rebind(name, value);
        } catch (RemoteException excp) {
            throw new RuntimeException("unexpected exception in "
                                       + "SimpleObjectRegistry.rebind: "
                                       + excp);
        }
    }

    /** Remove any binding of NAME in this registry. */
    public synchronized void unbind(String name) {
        if (_registry == null) {
            throw new IllegalStateException("registry not active");
        }
        try {
            _registry.unbind(name);
        } catch (NotBoundException e) {
            /* Ignore NotBoundException */
        } catch (RemoteException e) {
            throw new RuntimeException("unexpected exception in "
                                       + "SimpleObjectRegistry.unbind: " + e);
        }
    }

    /** Remove all bindings in this registry. */
    public synchronized void unbind() {
        if (_registry == null) {
            return;
        }

        try {
            for (String name : _registry.list()) {
                unbind(name);
            }
        } catch (RemoteException e) {
            /* Ignore RemoteException */
        }
    }

    /** Remove all bindings from this registry, and disable it from further
     *  use.  */
    public synchronized void close() {
        try {
            unbind();
        } finally {
            _freePorts.add(_port);
            _registry = null;
            _port = -1;
        }
    }

    /** Ports used for registries that have been closed. */
    private static ArrayList<Integer> _freePorts = new ArrayList<>();

    /** THe registry that I wrap. */
    private Registry _registry;
    /** The port number used for this registry. */
    private int _port;
}
