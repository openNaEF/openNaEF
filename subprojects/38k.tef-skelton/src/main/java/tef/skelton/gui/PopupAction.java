package tef.skelton.gui;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public abstract class PopupAction extends AbstractAction {

    protected PopupAction(String name, JPopupMenu popup) {
        super(name);
        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                setEnabled(isEnable());
            }

            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    abstract protected boolean isEnable();
}
