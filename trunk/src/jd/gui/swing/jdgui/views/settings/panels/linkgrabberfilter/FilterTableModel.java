package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.HostPluginWrapper;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.jdownloader.controlling.LinkFilter;
import org.jdownloader.controlling.LinkFilter.Types;
import org.jdownloader.controlling.LinkFilterOperator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<LinkFilter> {

    private static final long serialVersionUID = -7756459932564776739L;

    public FilterTableModel(String id) {
        super(id);
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<LinkFilter>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(LinkFilter value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, LinkFilter object) {
                object.setEnabled(value);
            }
        });

        String[] combo = new String[LinkFilter.Types.values().length];
        for (int i = 0; i < combo.length; i++) {
            switch (LinkFilter.Types.values()[i]) {
            case FILENAME:
                combo[i] = _GUI._.settings_linkgrabber_filter_types_filename();
                break;
            case PLUGIN:
                combo[i] = _GUI._.settings_linkgrabber_filter_types_plugin();
                break;
            case URL:
                combo[i] = _GUI._.settings_linkgrabber_filter_types_url();
                break;
            default:
                combo[i] = LinkFilter.Types.values()[i].name();
            }
        }

        final HostPluginWrapper[] options = HostPluginWrapper.getHostWrapper().toArray(new HostPluginWrapper[] {});
        final HashMap<String, HostPluginWrapper> map = new HashMap<String, HostPluginWrapper>();
        for (int i = 0; i < options.length; i++) {
            map.put(options[i].getHost(), options[i]);
            map.put(options[i].getPattern() + "", options[i]);
        }
        this.addColumn(new ExtComboColumn<LinkFilter>(_GUI._.settings_linkgrabber_filter_columns_type(), new DefaultComboBoxModel(combo)) {

            private static final long serialVersionUID = -4412876997430100402L;

            @Override
            protected int getSelectedIndex(LinkFilter value) {
                return value.getType().ordinal();
            }

            public boolean isEnabled(LinkFilter obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 90;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            public boolean isSortable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setSelectedIndex(int value, LinkFilter object) {
                Types nValue = LinkFilter.Types.values()[value];
                if (object.getType() == LinkFilter.Types.PLUGIN && nValue != LinkFilter.Types.PLUGIN) {
                    HostPluginWrapper conv = map.get(object.getRegex());
                    if (conv != null) {
                        object.setRegex(conv.getPattern() + "");
                        object.setFullRegex(true);
                    }

                } else if (nValue == LinkFilter.Types.PLUGIN && object.getType() != LinkFilter.Types.PLUGIN) {
                    HostPluginWrapper conv = map.get(object.getRegex());
                    if (conv != null) {
                        object.setRegex(conv.getHost() + "");
                        object.setFullRegex(false);
                    }
                }
                object.setType(nValue);

            }

        });

        this.addColumn(new ExtComboColumn<LinkFilter>(_GUI._.settings_linkgrabber_filter_columns_blackwhite(), new DefaultComboBoxModel(new String[] { _GUI._.settings_linkgrabber_filter_columns_blackwhite_contains_not(), _GUI._.settings_linkgrabber_filter_columns_blackwhite_contains(), _GUI._.settings_linkgrabber_filter_columns_blackwhite_is(), _GUI._.settings_linkgrabber_filter_columns_blackwhite_is_not() })) {

            private static final long serialVersionUID = 8475648905225363397L;

            @Override
            protected int getSelectedIndex(LinkFilter value) {
                return value.getOperator().ordinal();
            }

            public boolean isEnabled(LinkFilter obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 90;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            public boolean isSortable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setSelectedIndex(int value, LinkFilter object) {

                object.setOperator(LinkFilterOperator.values()[value]);

            }

        });
        this.addColumn(new FilterColumn());

        this.addColumn(new ExtCheckColumn<LinkFilter>(_GUI._.settings_linkgrabber_filter_columns_advanced()) {
            private static final long serialVersionUID = -9104378643478603148L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 2051980596953422289L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("regex", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(LinkFilter value) {
                return value.isFullRegex();
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return obj.getType() != LinkFilter.Types.PLUGIN;

            }

            @Override
            public boolean isEnabled(LinkFilter obj) {
                return obj.getType() != LinkFilter.Types.PLUGIN && obj.isEnabled();
            }

            @Override
            protected void setBooleanValue(boolean value, LinkFilter object) {
                object.setFullRegex(value);

            }

        });
    }
}
