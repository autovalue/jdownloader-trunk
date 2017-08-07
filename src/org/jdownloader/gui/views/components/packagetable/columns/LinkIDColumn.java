package org.jdownloader.gui.views.components.packagetable.columns;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtTextAreaColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public class LinkIDColumn extends ExtTextAreaColumn<AbstractNode> {
    /**
     *
     */
    private static final long serialVersionUID = -5306908011438156399L;

    public LinkIDColumn() {
        super(_GUI.T.LinkIDColumn_LinkIDColumn());
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode) {
            return ((AbstractPackageNode) obj).getView().isEnabled();
        } else {
            return obj != null && obj.isEnabled();
        }
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        /* needed so we can edit even is row is disabled */
        return isEditable(obj);
    }

    @Override
    public String getStringValue(AbstractNode object) {
        final DownloadLink dl;
        if (object instanceof DownloadLink) {
            dl = (DownloadLink) object;
        } else if (object instanceof CrawledLink) {
            dl = ((CrawledLink) object).getDownloadLink();
        } else {
            return null;
        }
        if (dl != null) {
            return dl.getLinkID();
        } else {
            return null;
        }
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }
}
