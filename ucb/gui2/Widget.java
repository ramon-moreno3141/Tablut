package ucb.gui2;

import javax.swing.JComponent;
import java.util.Observable;

/** The parent class of all additional things that may be added to a
 *  TopLevel.
 *  @author P. N. Hilfinger */
public abstract class Widget extends Observable {

    /** Request that I get the focus.  Returns false if request is
     *  certain to fail, and true otherwise. */
    public boolean requestFocusInWindow() {
        return me.requestFocusInWindow();
    }

    /** The component representing me. */
    protected JComponent me;
}
