package ucb.gui;

import javax.swing.JComponent;

/** The parent class of all additional things that may be added to a
 *  TopLevel.
 *  @author P. N. Hilfinger */
public abstract class Widget {

    /** Request that I get the focus.  Returns false if request is
     *  certain to fail, and true otherwise. */
    public boolean requestFocusInWindow() {
        return me.requestFocusInWindow();
    }

    /** The component representing me. */
    protected JComponent me;
}


