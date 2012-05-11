package org.jdownloader.gui.views.downloads.table;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.views.SelectionInfo;

public class DownloadTableContext extends MenuContext<JPopupMenu> {

    private ExtColumn<AbstractNode>                  clickedColumn;
    private SelectionInfo<FilePackage, DownloadLink> selectionInfo;

    public SelectionInfo<FilePackage, DownloadLink> getSelectionInfo() {
        return selectionInfo;
    }

    public DownloadTableContext(JPopupMenu popup, SelectionInfo<FilePackage, DownloadLink> si, ExtColumn<AbstractNode> column) {
        super(popup);

        this.selectionInfo = si;
        clickedColumn = column;

    }

    public ExtColumn<AbstractNode> getClickedColumn() {
        return clickedColumn;
    }

}
