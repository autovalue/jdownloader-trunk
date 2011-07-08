package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AuthTable         table;

    public NewAction(AuthTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_auth_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 16));
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                AuthenticationController.getInstance().add(new AuthenticationInfo());
                table.getExtTableModel()._fireTableStructureChanged(AuthenticationController.getInstance().list(), false);
            }
        });

    }

}
