//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.downloadview;

import jd.controlling.DownloadController;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.Columns.DateAddedColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.DateFinishedColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.FileColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.HosterColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.PartColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.ProgressColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.SizeColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

public class DownloadJTableModel extends JDTableModel {

    private static final long serialVersionUID = 1L;

    public DownloadJTableModel(String configname) {
        super(configname);
        this.addColumn(new FileColumn(JDL.L("gui.treetable.name", "F"), this));
        this.addColumn(new PartColumn(JDL.L("gui.treetable.part", "Part"), this));
        this.addColumn(new SizeColumn(JDL.L("gui.treetable.size", "FileSize"), this));
        this.addColumn(new HosterColumn(JDL.L("gui.treetable.hoster", "Anbieter"), this));
        this.addColumn(new DateAddedColumn(JDL.L("gui.treetable.added", "Added date"), this));
        this.addColumn(new DateFinishedColumn(JDL.L("gui.treetable.finished", "Finished date"), this));
        this.addColumn(new StatusColumn(JDL.L("gui.treetable.status", "Status"), this));
        this.addColumn(new ProgressColumn(JDL.L("gui.treetable.progress", "Fortschritt"), this));
    }

    public void refreshModel() {
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                synchronized (list) {
                    list.clear();
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        list.add(fp);
                        if (fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                list.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }
}
