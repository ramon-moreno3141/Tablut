package ucb.gui2;

import java.util.HashMap;
import java.util.Observable;

import java.util.function.Consumer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

/** A top-level window with optional menu bar.  A TopLevel primarily
 *  exists to contain Widgets.  The general technique is to extend
 *  TopLevel.  Most of the methods here are protected: the intent is that
 *  any calls to them are from within your extension class, thus allowing
 *  the latter to retain control over its size, menu, etc.
 *  @author P. N. Hilfinger */
public class TopLevel extends Observable implements ActionListener {

    /** If VISIBLE, display this TopLevel.  Otherwise, make it
     *  invisible. */
    public void display(boolean visible) {
        if (visible) {
            frame.pack();
        }
        frame.setVisible(visible);
    }

    /** A new TopLevel with the given TITLE (which window managers
     *  typically display on the border).  If EXITONCLOSE, then
     *  closing this window exits the application. */
    protected TopLevel(String title, boolean exitOnClose) {
        frame = new JFrame(title);
        frame.setUndecorated(true);
        frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        frame.setLayout(new GridBagLayout());
        if (exitOnClose) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    /** Set my preferred size to WIDTH x HEIGHT pixels. */
    public void setPreferredSize(int width, int height) {
        frame.setPreferredSize(new Dimension(width, height));
    }

    /** Set my minimum size to WIDTH x HEIGHT pixels. */
    public void setMinimumSize(int width, int height) {
        frame.setMinimumSize(new Dimension(width, height));
    }

    /** Set my maximum size to WIDTH x HEIGHT pixels. */
    public void setMaximumSize(int width, int height) {
        frame.setMaximumSize(new Dimension(width, height));
    }

    /** Return my current width in pixels. */
    public int getWidth() {
        return frame.getWidth();
    }

    /** Return my current height in pixels. */
    public int getHeight() {
        return frame.getHeight();
    }

    /** Add a new simple menu button labeled LABEL to my menus, which
     *  when clicked, invokes FUNC, sending the button itself and LABEL
     *  as arguments.  A null value of FUNC indicates no action. LABEL
     *  has the form MENUNAME->SUBMENU1->...SUBMENUn->NAME, where n >= 0,
     *  (for example, "File->Open" or "File->New->Project").  This
     *  label denotes "the item labeled NAME in the submenu named
     *  SUBMENUn in the ... in the menu-bar entry MENUNAME.
     *  The new button appears at the end of its menu.  Likewise, any
     *  previously non-existing menus get created at the end of the
     *  menu bar or their containing menu.  */
    protected void addMenuButton(String label, Consumer<String> func) {
        String[] names = label.split("->");
        if (names.length <= 1) {
            throw new IllegalArgumentException("cannot parse label");
        }
        JMenu menu = getMenu(names, names.length - 2);
        JMenuItem item = new JMenuItem(names[names.length - 1]);
        item.setActionCommand(label);
        item.addActionListener(this);
        menu.add(item);
        buttonMap.put(label, new ButtonHandler(func, label, item));
    }

    /** Add a check box labeled LABEL to my menus, which when clicked,
     *  flips its state and invokes FUNC, sending the button itself and
     *  LABEL as arguments.  LABEL is as for addMenuButton.  The box is
     *  initially checked iff CHECKED. A null value of FUNC indicates
     *  no action (aside from changing selection state). */
    protected void addMenuCheckBox(String label, boolean selected,
                                   Consumer<String> func) {
        String[] names = label.split("->");
        if (names.length <= 1) {
            throw new IllegalArgumentException("cannot parse label");
        }
        JMenu menu = getMenu(names, names.length - 2);
        JMenuItem item =
            new JCheckBoxMenuItem(names[names.length - 1], selected);
        item.setActionCommand(label);
        item.addActionListener(this);
        menu.add(item);
        buttonMap.put(label, new ButtonHandler(func, label, item));
    }        

    /** Add a radio button labeled LABEL to my menus, belonging to the
     *  group of buttons called GROUPNAME.  Initially, the button is
     *  selected iff SELECTED.  Only one radio button in a group is selected
     *  at a time (see {@link #isSelected}); when the user clicks one, it
     *  becomes selected and any other button in the group is deselected.
     *  If FUNC is non-null, it denotes a function that is invoked
     *  when the button is pressed, sending the button and LABEL as
     *  its arguments.  */
    protected void addMenuRadioButton(String label, String groupName,
                                      boolean selected, Consumer<String> func) {
        String[] names = label.split("->");
        if (names.length <= 1) {
            throw new IllegalArgumentException("cannot parse label");
        }
        JMenu menu = getMenu(names, names.length - 2);
        JMenuItem item =
            new JRadioButtonMenuItem(names[names.length - 1], selected);
        getGroup(groupName).add(item);
        item.setActionCommand(label);
        buttonMap.put(label, new ButtonHandler(func, label, item));
        if (func != null) {
            item.addActionListener(this);
        }
        menu.add(item);
    }

    /** Add a separator to the end of the menu labeled LABEL (which
     *  must exist) in my menu bar. LABEL has the form
     *  MENUNAME->SUBMENU1->...->SUBMENUn. */
    protected void addSeparator(String label) {
        String[] labels = label.split("->");
        if (labels.length == 0) {
            throw new IllegalArgumentException("invalid menu designator");
        }
        JMenu menu = getMenu(labels, labels.length - 1);
        menu.addSeparator();
    }

    /** Return true iff the button named LABEL is currently selected. */
    protected boolean isSelected(String label) {
        ButtonHandler h = buttonMap.get(label);
        if (h == null) {
            return false;
        }
        return h.isSelected();
    }

    /** Set isSelected(LABEL) to VAL, if LABEL is a valid button. */
    protected void select(String label, boolean val) {
        ButtonHandler h = buttonMap.get(label);
        if (h != null) {
            h.setSelected(val);
        }
    }

    /** Set the enabled status of the buttons labeled LABELS[0], ...  to ENABLE.
     *  An ENABLE value of false causes the buttons to become unresponsive,
     *  typically displaying as being grayed out. */
    protected void setEnabled(boolean enable, String... labels) {
        for (String label : labels) {
            ButtonHandler h = buttonMap.get(label);
            if (h != null) {
                h.setEnabled(enable);
            }
        }
    }

    /** Add a new button displaying LABEL, laid out according to
     *  LAYOUT, which when clicked calls FUNC, with the button and
     *  LABEL as its arguments.  */
    protected void addButton(String label, Consumer<String> func,
                             LayoutSpec layout) {
        if (buttonMap.containsKey(label)) {
            throw new IllegalStateException("already have button labeled "
                                            + label);
        }
        JButton button = new JButton(label);
        button.setActionCommand(label);
        button.addActionListener(this);
        frame.add(button, layout.params());
        buttonMap.put(label, new ButtonHandler(func, label, button));
    }

    /** Add a new check box displaying LABEL, laid out according to
     *  LAYOUT, which when clicked calls FUNC, with the button and
     *  LABEL as its arguments.  It is initially selected iff SELECTED. */
    protected void addCheckBox(String label, boolean selected,
                               Consumer<String> func, LayoutSpec layout) {
        if (buttonMap.containsKey(label)) {
            throw new IllegalStateException("already have item labeled "
                                            + label);
        }
        JCheckBox box = new JCheckBox(label, selected);
        box.setActionCommand(label);
        box.addActionListener(this);
        frame.add(box, layout.params());
        buttonMap.put(label, new ButtonHandler(func, label, box));
    }

    /** Add a radio button labeled LABEL, placed according to LAYOUT,
     *  belonging to the group of buttons called GROUPNAME.  Initially,
     *  the button is selected iff SELECTED.  Only one radio button in
     *  a group is selected at a time (see {@link #isSelected}); when the
     *  user clicks one, it becomes selected and any other button in the
     *  group is deselected. If FUNC is non-null, FUNC is invoked when
     *  pressed, sending the LABEL as argument.  */
    protected void addRadioButton(String label, String groupName,
                                  boolean selected, Consumer<String> func,
                                  LayoutSpec layout) {
        JRadioButton item = new JRadioButton(label, selected);
        getGroup(groupName).add(item);
        item.setActionCommand(label);
        buttonMap.put(label, new ButtonHandler(func, label, item));
        if (func != null) {
            item.addActionListener(this);
        }
        frame.add(item, layout.params());
    }

    /** Add WIDGET, placed according to LAYOUT. */
    protected void add(Widget widget, LayoutSpec layout) {
        frame.add(widget.me, layout.params());
    }

    /** Add a label that initially displays the text TEXT, placed
     *  according to LAYOUT, and identified by the tag ID.  If a label
     *  with the same ID already exists, its text is altered to TEXT. */
    protected void addLabel(String text, String id, LayoutSpec layout) {
        if (labelMap.containsKey(id)) {
            throw new IllegalArgumentException("duplicate label id: "
                                               + id);
        }
        JLabel label = new JLabel(text);
        labelMap.put(id, label);
        frame.add(label, layout.params());
    }

    /** Set the text of the existing label with tag ID to TEXT. */
    protected void setLabel(String id, String text) {
        JLabel label = labelMap.get(id);
        if (label == null) {
            throw new IllegalArgumentException("unknown label id: "
                                               + id);
        }
        label.setText(text);
    }


    /** Add a new, anonymous label that displays the text TEXT, placed
     *  according to LAYOUT. */
    protected void addLabel(String text, LayoutSpec layout) {
        frame.add(new JLabel(text), layout.params());
    }

    /** Display the dismissable message TEXT of type TYPE in
     *  a separate dialog window with title TITLE.  TYPE may be any
     *  of the strings "information", "warning", "error", or "plain",
     *  which modify  the look of the message.  */
    public void showMessage(String text, String title, String type) {
        JOptionPane.showMessageDialog(frame, text, title,
                                      getMessageType(type), null);
    }

    /** Display a choice of optional responses, labeled LABELS[0],...,LABELS[n]
     *  in a separate dialog box with title TITLE and message MESSAGE.
     *  Returns the selected option (0 -- n), or -1 if the user closes the
     *  dialog window.  DEFLT is the default label (may be null).
     *  TYPE may be "question", "information",  "warning", "error",
     *  or "plain". */
    public int showOptions(String message, String title, String type,
                           String deflt, String... labels) {
        return JOptionPane.showOptionDialog(frame, message, title,
                                            0, getMessageType(type),
                                            null, labels, deflt);
    }

    /** Display a dialog box with message MESSAGE and title TITLE that
     *  prompts the user for textual input, with INIT providing the
     *  initial value of the text.  TYPE may be "question",
     *  "information",  "warning", "error", or "plain".   Return the user's
     *  input text, or null if the user closes the dialog window. */
    public String getTextInput(String message, String title, String type,
                               String init) {
        Object input = JOptionPane.showInputDialog(frame, message, title,
                                                   getMessageType(type),
                                                   null, null, init);
        if (input instanceof String) {
            return (String) input;
        } else {
            return null;
        }
    }

    /** When the focus is in my window, request that WIDGET, which should
     *  be one of my components, get the focus. */
    public void setPreferredFocus(final Widget widget) {
        frame.addWindowFocusListener(new WindowAdapter() {
                public void windowGainedFocus(WindowEvent e) {
                    widget.requestFocusInWindow();
                }
            });
    }

    /** Return the JOptionPane option value corresponding to a message type
     *  of TYPE (case-insensitive).  */
    private int getMessageType(String type) {
        if (type == null) {
            return JOptionPane.PLAIN_MESSAGE;
        }
        type = type.toLowerCase();
        int intType;
        intType = JOptionPane.PLAIN_MESSAGE;
        if (type != null && MESSAGE_TYPE_MAP.containsKey(type)) {
            intType = MESSAGE_TYPE_MAP.get(type);
        }
        return intType;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof AbstractButton) {
            String key = e.getActionCommand();
            ButtonHandler h = buttonMap.get(key);
            if (h == null) {
                return;
            }
            h.doAction();
        }
    }

    /** Return the (sub)menu LABEL[0] -> LABEL[1] -> LABEL[LAST] .... */
    private JMenu getMenu(String[] label, int last) {
        if (frame.getJMenuBar() == null) {
            frame.setJMenuBar(new JMenuBar());
        }
        JMenuBar bar = frame.getJMenuBar();
        JMenu menu;
        menu = null;
        for (int i = 0; i < bar.getMenuCount(); i += 1) {
            menu = bar.getMenu(i);
            if (menu.getText().equals(label[0])) {
                break;
            }
            menu = null;
        }
        if (menu == null) {
            menu = new JMenu(label[0]);
            bar.add(menu);
        }
        for (int k = 1; k <= last; k += 1) {
            JMenu menu0 = menu;
            menu = null;
            for (int i = 0; i < menu0.getItemCount(); i += 1) {
                JMenuItem item = menu0.getItem(i);
                if (item == null) {
                    continue;
                }
                if (item.getText().equals(label[k])) {
                    if (item instanceof JMenu) {
                        menu = (JMenu) item;
                        break;
                    } else {
                        throw new IllegalStateException("inconsistent menu "
                                                        + "label");
                    }
                }
                menu = null;
            }
            if (menu == null) {
                menu = new JMenu(label[k]);
                menu0.add(menu);
            }
        }
        return menu;
    }

    /** Return the group named NAME. */
    private ButtonGroup getGroup(String name) {
        ButtonGroup g = buttonGroups.get(name);
        if (g == null) {
            g = new ButtonGroup();
            buttonGroups.put(name, g);
        }
        return g;
    }

    /** Mouse and key event handler. */
    private static class ButtonHandler {
        /** A handler for events from SRC that are identified by ID with
         *  that FUNC. */
        ButtonHandler(Consumer<String> func, String id, AbstractButton src) {
            _src = src;
            _id = id;
            _func = func;
        }

        /** Return true iff my source button is selected. */
        boolean isSelected() {
            return _src.getModel().isSelected();
        }

        /** Set the value of isSelected for my source button to VALUE. */
        void setSelected(boolean value) {
            _src.setSelected(value);
        }

        /** Set the enabled status of my source button to VALUE. */
        void setEnabled(boolean value) {
            _src.setEnabled(value);
        }

        /** Perform the action indicated for clicking this button. */
        void doAction() {
            if (_func != null) {
                _func.accept(_id);
            }
        }

        /** Method to call. */
        private Consumer<String> _func;
        /** Event type identifier. */
        private String _id;
        /** Source button. */
        private AbstractButton _src;
    }

    /** Map from event identifiers to button handlers. */
    private final HashMap<String, ButtonHandler> buttonMap = new HashMap<>();
    /** Map from group names button groups. */
    private final HashMap<String, ButtonGroup> buttonGroups = new HashMap<>();
    /** Map from label identifiers to labels. */
    private final HashMap<String, JLabel> labelMap = new HashMap<>();

    /** Map from message type names ("information", "warning", etc.) to
     *  their internal identifying integer values. */
    private static final HashMap<String, Integer> MESSAGE_TYPE_MAP =
        new HashMap<>();

    static {
        MESSAGE_TYPE_MAP.put("information", JOptionPane.INFORMATION_MESSAGE);
        MESSAGE_TYPE_MAP.put("warning", JOptionPane.WARNING_MESSAGE);
        MESSAGE_TYPE_MAP.put("error", JOptionPane.ERROR_MESSAGE);
        MESSAGE_TYPE_MAP.put("plain", JOptionPane.PLAIN_MESSAGE);
        MESSAGE_TYPE_MAP.put("information", JOptionPane.INFORMATION_MESSAGE);
        MESSAGE_TYPE_MAP.put("question", JOptionPane.QUESTION_MESSAGE);
    }

    /** The Swing frame representing this TopLevel object. */
    protected final JFrame frame;
}
