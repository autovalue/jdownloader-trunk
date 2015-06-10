package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import org.appwork.utils.StringUtils;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.statistics.StatsManager;

public class OpenURLAction extends AppAction {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7939070621339510855L;
    private DomainInfo        info;
    private String            id;

    public OpenURLAction(DomainInfo info, String id) {
        super();
        this.id = id;
        setName(_GUI._.OpenURLAction_OpenURLAction_());
        this.info = info;
    }

    public void actionPerformed(ActionEvent e) {
        String url = null;
        try {
            url = info.findPlugin().getBuyPremiumUrl();
        } catch (final Throwable e2) {
            LogController.CL().log(e2);
        }
        if (StringUtils.isEmpty(url)) {
            url = info.getTld();
        }
        StatsManager.I().openAfflink(url, id, false);

    }
}
