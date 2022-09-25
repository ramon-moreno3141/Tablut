package ucb.util;

/** A simple timer class that periodically performs some action.
 *  @author P. N. Hilfinger */
public abstract class Ticker implements Runnable {

    /** A Ticker that, when started, fires every INTERVAL milliseconds, or
     *  never, if interval <= 0. */
    public Ticker(long interval) {
        _interval = interval;
        _running = _done = false;
        _clock = new Thread(this);
        _clock.setPriority(Thread.MAX_PRIORITY);
        if (interval > 0) {
            _clock.start();
        }
    }

    /** Start ticking, executing tick() at each tick. */
    public synchronized void start() {
        _running = true;
        notifyAll();
    }

    /** Stop ticking. */
    public synchronized void stop() {
        _running = false;
        _clock.interrupt();
    }

    /** Permanently stop THIS and terminate its thread. */
    public synchronized void close() {
        _running = false;
        _done = true;
        _clock.interrupt();
    }

    /** Action to perform on each clock tick. Must be overrridden. */
    protected abstract void tick();

    /** Internal method.  Don't call directly. */
    public synchronized void run() {
        while (!_done) {
            try {
                if (_running) {
                    wait(_interval);
                    tick();
                } else {
                    wait();
                }
            } catch (InterruptedException e) {
                /* Ignore InterruptedException */
            }
        }
    }

    /** Interval between ticks. */
    private long _interval;
    /** True if ticking. */
    private boolean _running;
    /** Set true when Ticker is closed. */
    private boolean _done;
    /** The Thread that issues ticks. */
    private Thread _clock;

}
