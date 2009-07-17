package jd.gui.skins.jdgui.views;



import javax.swing.Icon;

import jd.gui.skins.jdgui.views.info.DownloadInfoPanel;
import jd.gui.skins.simple.JDToolBar;
import jd.gui.skins.simple.components.DownloadView.DownloadLinksPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class DownloadView extends View{

    public DownloadView() {
        super();
//        this.setToolBar(new JDToolBar());
//        this.setSideBar(new DownloadTaskPane(JDL.L("gui.taskpanes.download", "Download"), JDTheme.II("gui.images.taskpanes.download", 16, 16)));
        this.setContent(new DownloadLinksPanel());
this.setInfo(new DownloadInfoPanel());
    }
  
 

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.downloadview.";

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.taskpanes.download", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.title", "Download");
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Downloadlist and Progress");
    }

}
