package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {
    private Configuration  configuration;

    private HTTPLiveHeader lh;

    private GUIConfigEntry routerScript;
    private GUIConfigEntry ip;
    private GUIConfigEntry user;
    private GUIConfigEntry pass;

    private JButton btnAutoConfig;

    private JButton btnSelectRouter;

    public SubPanelLiveHeaderReconnect(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        this.lh = (HTTPLiveHeader) interaction;
        load();
    }

    public void save() {
        this.saveConfigEntries();

    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        ConfigEntry cfg;
        btnSelectRouter = new JButton(JDLocale.L("gui.config.liveHeader.selectRouter", "Router auswählen"));
        btnAutoConfig = new JButton(JDLocale.L("gui.config.liveHeader.autoConfig","Router automatisch setzten"));
        btnSelectRouter.addActionListener(this);
        btnAutoConfig.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnSelectRouter, 0, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnAutoConfig, 1, 1, GridBagConstraints.REMAINDER, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        user = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(pass);
        String routerip = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if(routerip==null)
        {
            GetRouterInfo rinfo = new GetRouterInfo();
            routerip=rinfo.getAdress();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, routerip);
            JDUtilities.saveConfig();
        }
        ip = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")).setDefaultValue(routerip));
        addGUIConfigEntry(ip);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, JDLocale.L("gui.config.liveHeader.waitTimeForIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, JDLocale.L("gui.config.liveHeader.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, JDLocale.L("gui.config.liveHeader.waitForIP", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20).setExpertEntry(true));
        addGUIConfigEntry(ce);
        routerScript = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, JDLocale.L("gui.config.liveHeader.script", "HTTP Script")));
        addGUIConfigEntry(routerScript);

        add(panel);

    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.liveHeader.name", "Reconnect via LiveHeader");
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSelectRouter) {
        Vector<String[]> scripts = lh.getLHScripts();

        Collections.sort(scripts, new Comparator<Object>() {
            public int compare(Object a, Object b) {
                String[] aa = (String[]) a;
                String[] bb = (String[]) b;

                if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) > 0) {
                    return 1;
                }
                else if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) < 0) {
                    return -1;
                }
                else {
                    return 0;
                }

            }

        });

        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = scripts.size() - 1; i >= 0; i--) {
            if (ch.containsKey(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2])) {
                scripts.remove(i);
            }
            else {

                ch.put(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2], true);
            }
        }
        ch.clear();
        String[] d = new String[scripts.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = i + ". " + JDUtilities.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
        }

        String selected = (String) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.liveHeader.dialog.selectRouter", "Bitte wähle deinen Router aus"), JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"), JOptionPane.INFORMATION_MESSAGE, null, d, null);
        if (selected != null) {

            int id = Integer.parseInt(selected.split("\\.")[0]);
            String[] data = scripts.get(id);
            if (data[2].toLowerCase().indexOf("curl") >= 0) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
            }
            routerScript.setData(data[2]);
            String username = (String) user.getText();
            if(username==null || username.matches("[\\s]*"))
                user.setData(data[4]);
            String pw = (String) pass.getText();
            if(pw==null || pw.matches("[\\s]*"))
                pass.setData(data[5]);
                

        }
        }
        else
        {
            GetRouterInfo routerInfo = new GetRouterInfo();
            String username = (String) user.getText();
            if(username!=null && !username.matches("[\\s]*"))
                routerInfo.username=username;
            String pw = (String) pass.getText();
            if(pw!=null && !pw.matches("[\\s]*"))
                routerInfo.password=pw;
            String[] data = routerInfo.getRouterData();
            if(data==null)
            {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.notFound", "JD can't your router plese select the router manual!"));
                return;
            }
            routerScript.setData(data[2]);
            if(username==null || username.matches("[\\s]*"))
                user.setData(data[4]);
            if(pw==null || pw.matches("[\\s]*"))
                pass.setData(data[5]);

                user.setData(data[4]);
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.yourRouter", "Sie haben eine")+" "+data[1]);
        }

    }

}
