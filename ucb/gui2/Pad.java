package ucb.gui2;

import java.util.HashMap;

import java.util.function.BiConsumer;

import javax.swing.JComponent;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

/** A Pad is a blank slate that may be inserted into a TopLevel.  It
 *  provides methods that set up responses to mouse events, and an
 *  overrideable paintComponent method that allows one to draw anything
 *  that may be rendered on a standard Java Graphics2D.  Standard
 *  usage is to extend Pad.
 * @author P. N. Hilfinger */
public class Pad extends Widget {

    /** A new, empty Pad. */
    protected Pad() {
        me = new PadComponent();
        me.setFocusable(true);
        for (String event : MOUSE_EVENT_NAMES) {
            mouseEventMap.put(event, null);
        }
        for (String event : KEY_EVENT_NAMES) {
            keyEventMap.put(event, null);
        }
    }

    /** Set my current size to WIDTH x HEIGHT pixels. */
    public void setSize(int width, int height) {
        me.setSize(width, height);
    }

    /** Set my preferred size to WIDTH x HEIGHT pixels. */
    public void setPreferredSize(int width, int height) {
        preferredSize = new Dimension(width, height);
        me.invalidate();
    }

    /** Set my minimum size to WIDTH x HEIGHT pixels. */
    public void setMinimumSize(int width, int height) {
        minimumSize = new Dimension(width, height);
        me.invalidate();
    }

    /** Set my maximum size to WIDTH x HEIGHT pixels. */
    public void setMaximumSize(int width, int height) {
        maximumSize = new Dimension(width, height);
        me.invalidate();
    }

    /** Return my current width in pixels. */
    public int getWidth() {
        return me.getWidth();
    }

    /** Return my current height in pixels. */
    public int getHeight() {
        return me.getHeight();
    }

    /** Alert the system that I ought be asked to repaint myself. */
    public void repaint() {
        me.repaint();
    }

    /** Alert the system that I ought be asked to repaint myself
     *  within TM milliseconds. */
    public void repaint(long tm) {
        me.repaint(tm);
    }

    /** Alert the system that I ought be asked to repaint the rectangular
     *  area whose top left corner is X, Y and has given WIDTH and
     *  HEIGHT. */
    public void repaint(int x, int y, int width, int height) {
        me.repaint(x, y, width, height);
    }

    /** Alert the system that I ought be asked to repaint the rectangular
     *  area whose top left corner is X, Y and has given WIDTH and
     *  HEIGHT within TM milliseconds. */
    public void repaint(long tm, int x, int y, int width, int height) {
        me.repaint(tm, x, y, width, height);
    }

    /** Handle the kind of mouse event EVENTKIND by calling FUNC's accept
     *  method with EVENT and the MouseEvent that describes the event.
     *  The default handling of any event is to ignore it.  The
     *  possible values of EVENTKIND are:
     *  <ul>
     *  <li> "click": A mouse button is clicked (pressed and released).
     *  <li> "press": A mouse button is pressed.
     *  <li> "release": A mouse button is released.
     *  <li> "enter": The mouse enters this Pad.
     *  <li> "exit": The mouse exits this Pad.
     *  <li> "drag": The mouse dragged (moved while a button is pressed).
     *  <li> "move": The mouse moves.
     *  </ul>
     *  Mouse clicks follow releases (both occur), unless the mouse
     *  has been dragged, in which case one sees a press, one or more
     *  drags, and a release with no click.

     *  @see java.awt.event.MouseEvent.
     */
    public void setMouseHandler(String eventKind,
                                BiConsumer<String, MouseEvent> func) {
        if (!mouseEventMap.containsKey(eventKind)) {
            throw new IllegalArgumentException("Unknown event: " + eventKind);
        }
        mouseEventMap.put(eventKind, func);
        if (eventKind.equals("drag") || eventKind.equals("move")) {
            if (me.getMouseMotionListeners().length == 0) {
                me.addMouseMotionListener((MouseMotionListener) me);
            }
        } else if (me.getMouseListeners().length == 0) {
            me.addMouseListener((MouseListener) me);
        }
    }

    /** Handle the kind of key event EVENTKIND by calling FUNC's accept
     *  method with EVENT and the KeyEvent that describes the event.
     *  The default handling of any event is to ignore it.  The
     *  possible values of EVENTKIND are:
     *  <ul>
     *  <li> "keypress": A key is pressed.
     *  <li> "keyrelease": A key is released.
     *  <li> "keytype": A key is typed (denoting a valid Unicode character).
     *  </ul>
     *  Keytype events occur between a press and subsequent release
     *  event (all three occur if the key represents a Unicode
     *  character).  Keytype events do not occur for typing of keys
     *  such as Ctrl, Shift, Meta, Insert, or function keys (F1, etc.)
     *  which do not correspond to Unicode characters.
     */
    public void setKeyHandler(String eventKind,
                              BiConsumer<String, KeyEvent> func) {
        me.setFocusTraversalKeysEnabled(false);
        if (!keyEventMap.containsKey(eventKind)) {
            throw new IllegalArgumentException("Unknown event: " + eventKind);
        }
        keyEventMap.put(eventKind, func);
        if (me.getKeyListeners().length == 0) {
            me.addKeyListener((KeyListener) me);
        }
    }

    /** Repaint myself on G.  This default implementation does nothing. */
    protected void paintComponent(Graphics2D g) {
    }

    /** Dispatch handler for mouse event E of type KIND. */
    private void handle(String kind, MouseEvent e) {
        BiConsumer<String, MouseEvent> h = mouseEventMap.get(kind);
        if (h == null) {
            return;
        }
        h.accept(kind, e);
    }

    /** Dispatch handler for key event E of type KIND. */
    private void handle(String kind, KeyEvent e) {
        BiConsumer<String, KeyEvent> h = keyEventMap.get(kind);
        if (h == null) {
            return;
        }
        h.accept(kind, e);
    }

    /** My graphic area. */
    private class PadComponent extends JComponent
        implements MouseListener, MouseMotionListener, KeyListener {

        /** Draw me on G. */
        public void paintComponent(Graphics g) {
            Pad.this.paintComponent((Graphics2D) g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Pad.this.handle("click", e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Pad.this.handle("release", e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Pad.this.handle("press", e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            Pad.this.handle("enter", e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Pad.this.handle("exit", e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Pad.this.handle("drag", e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Pad.this.handle("move", e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            Pad.this.handle("keypress", e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            Pad.this.handle("keyrelease", e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            Pad.this.handle("keytype", e);
        }

        @Override
        public Dimension getPreferredSize() {
            if (preferredSize == null) {
                return super.getPreferredSize();
            } else {
                return preferredSize;
            }
        }

        @Override
        public Dimension getMinimumSize() {
            if (minimumSize == null) {
                return super.getMinimumSize();
            } else {
                return minimumSize;
            }
        }

        @Override
        public Dimension getMaximumSize() {
            if (maximumSize == null) {
                return super.getMaximumSize();
            } else {
                return maximumSize;
            }
        }

    }

    /** Mapping of event types to their handlers. */
    private final HashMap<String, BiConsumer<String, MouseEvent>>
        mouseEventMap = new HashMap<>();
    /** Mapping of event types to their handlers. */
    private final HashMap<String, BiConsumer<String, KeyEvent>>
        keyEventMap = new HashMap<>();
    /** Valid mouse event names. */
    private static final String[] MOUSE_EVENT_NAMES = {
        "press", "release", "click", "enter", "exit", "drag", "move"
    };

    /** Valid key event names. */
    private static final String[] KEY_EVENT_NAMES = {
        "keypress", "keyrelease", "keytype"
    };

    /** Size parameters. */
    private Dimension minimumSize, preferredSize, maximumSize;

}
