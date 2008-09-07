//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.CFGConfig;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import sun.misc.Service;

/**
 * @author JD-Team
 */

public class JDInit {

    private static Logger logger = JDUtilities.getLogger();

    public static void setupProxy() {
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false)) {
            // http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            // für evtl authentifizierung:
            // http://www.softonaut.com/2008/06/09/using-javanetauthenticator-for
            // -proxy-authentication/
            // nonProxy Liste ist unnötig, da ja eh kein reconnect möglich wäre
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PROXY_PORT, 8080)).toString();
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_HOST, host);

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS, pass);

            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
            logger.info("http-proxy: enabled " + user + ":" + pass + "@" + host + ":" + port);

            System.setProperty("http.proxyUserName", user);
            System.setProperty("http.proxyPassword", pass);

        } else {
            System.setProperty("http.proxyHost", "");
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_PROXY, false);

            System.setProperty("http.proxySet", "false");
            logger.info("http-proxy: disabled");

        }
    }

    public static void setupSocks() {
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {
            // http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html

            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");
            String host = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.SOCKS_HOST, "");
            String port = new Integer(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.SOCKS_PORT, 1080)).toString();

            System.setProperty("socksProxySet", "true");
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", port);
            System.setProperty("socksProxyUserName", user);
            System.setProperty("socksProxyPassword", pass);
            System.setProperty("socks.useProxy", "true");
            System.setProperty("socks.proxyHost", host);
            System.setProperty("socks.proxyPort", port);
            System.setProperty("socks.proxyUserName", user);
            System.setProperty("socks.proxyPassword", pass);

            logger.info("socks-proxy: enabled");

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_HOST, host);

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, true);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.SOCKS_PORT, port);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_USER_SOCKS, user);
            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.PROXY_PASS_SOCKS, pass);

        } else {
            System.setProperty("socksProxySet", "false");
            System.setProperty("socks.useProxy", "false");
            System.setProperty("socks.proxyHost", "");
            System.setProperty("socksProxyHost", "");

            CFGConfig.getConfig("WEBUPDATE").setProperty(Configuration.USE_SOCKS, false);

            logger.info("socks-proxy: disabled");
        }
    }

    private int cid = -1;

    private boolean installerVisible = false;

    private SplashScreen splashScreen;

    public JDInit() {
        this(null);
    }

    public JDInit(SplashScreen splashScreen) {
        this.splashScreen = splashScreen;
    }

    private void afterConfigIsLoaded() {

    }

    public void checkUpdate() {
        File updater = JDUtilities.getResourceFile("webupdater.jar");
        if (updater.exists()) {
            if (!updater.delete()) {
                logger.severe("Webupdater.jar could not be deleted. PLease remove JDHOME/webupdater.jar to ensure a proper update");
            }
        }
        if (JDUtilities.getResourceFile("webcheck.tmp").exists() && JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")).indexOf("(Revision" + JDUtilities.getRevision() + ")") > 0) {
            JDUtilities.getController().getUiInterface().showTextAreaDialog("Error", "Failed Update detected!", "It seems that the previous webupdate failed.\r\nPlease ensure that your java-version is equal- or above 1.5.\r\nMore infos at http://www.syncom.org/projects/jdownloader/wiki/FAQ.\r\n\r\nErrorcode: \r\n" + JDUtilities.getLocalFile(JDUtilities.getResourceFile("webcheck.tmp")));
            JDUtilities.getResourceFile("webcheck.tmp").delete();
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false);
        } else {

            Interaction.handleInteraction(Interaction.INTERACTION_APPSTART, false);
        }

        String hash = "";

        if (JDUtilities.getResourceFile("updatemessage.html").exists()) {
            hash = JDUtilities.getLocalHash(JDUtilities.getResourceFile("updatemessage.html"));
        }

        JDUtilities.getRunType();
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_HASH, "").equals(hash)) {
            logger.info("Returned from Update");

            if (splashScreen != null) {
                splashScreen.finish();
            }

            SimpleGUI.showChangelogDialog();

        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_HASH, hash);
        JDUtilities.saveConfig();
    }

    protected void createQueueBackup() {
        Vector<DownloadLink> links = JDUtilities.getController().getDownloadLinks();
        Iterator<DownloadLink> it = links.iterator();
        Vector<BackupLink> ret = new Vector<BackupLink>();
        while (it.hasNext()) {
            DownloadLink next = it.next();
            if (next.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                ret.add(new BackupLink(new File(next.getContainerFile()), next.getContainerIndex(), next.getContainer()));
            } else {
                ret.add(new BackupLink(next.getDownloadURL()));
            }

        }

        JDUtilities.getResourceFile("links.linkbackup").delete();
        JDUtilities.saveObject(null, ret, JDUtilities.getResourceFile("links.linkbackup"), "links.linkbackup", "linkbackup", false);
    }

    public void doWebupdate(final int oldCid, final boolean guiCall) {
        CFGConfig cfg = CFGConfig.getConfig("WEBUPDATE");

        cfg.setProperty("PLAF", JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty("PLAF"));
        cfg.save();

        new Thread() {

            public void run() {
                ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title", "Webupdate"), 100);
                PackageManager pm = new PackageManager();
                ArrayList<PackageData> packages = pm.getDownloadedPackages();
                checkMessage();

                logger.finer("Init Webupdater");
                final WebUpdater updater = new WebUpdater(null);
                updater.setCid(oldCid);
                logger.finer("Get available files");
                // logger.info(files + "");
                Vector<Vector<String>> files;
                try {
                    files = updater.getAvailableFiles();
                } catch (Exception e) {
                    progress.setColor(Color.RED);
                    progress.setStatusText("Update failed");
                    progress.finalize(15000l);
                    return;
                }
                updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));
                // if(JDUtilities.getSubConfig("JAC").getBooleanProperty(
                // Configuration.USE_CAPTCHA_EXCHANGE_SERVER,
                // false)){
                // for (int i = files.size() - 1; i >= 0; i--) {
                //                  
                // // if
                // (files.get(i).get(0).startsWith("jd/captcha/methods/")&&files.
                // get(i).get(0).endsWith("mth"))
                // {
                // // logger.info("Autotrain active. ignore
                // "+files.get(i).get(0));
                // // files.remove(i);
                // // }
                // }
                // }
                if (files != null) {
                    JDUtilities.getController().setWaitingUpdates(files);
                }

                cid = updater.getCid();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
                JDUtilities.getSubConfig("GUI").setProperty(new String(new byte[] { 112, 97, 99, 107, 97, 103, 101 }), updater.sum);

                if (!guiCall && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    return;
                }

                if (files == null && packages.size() == 0) {
                    logger.severe("Webupdater offline");
                    progress.finalize();
                    return;
                }
                if (files == null) {
                    files = new Vector<Vector<String>>();
                }
                int org;
                progress.setRange(org = files.size());
                logger.finer("Files found: " + files);

                logger.finer("init progressbar");
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title", "Update Check"));
                if (files.size() > 0 || packages.size() > 0) {

                    progress.setStatus(org - (files.size() + packages.size()));
                    logger.finer("FIles to update: " + files);
                    logger.finer("JDUs to update: " + packages.size());

                    createQueueBackup();

                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.LF("init.webupdate.auto.countdowndialog", "Automatic update."), 10, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
                        if (ccd.result) {

                            Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");
                            // createDLCBackup();
                            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                            logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                            System.exit(0);
                        }
                    } else {

                        try {

                            CountdownConfirmDialog ccd = new CountdownConfirmDialog(JDUtilities.getGUI() != null ? ((SimpleGUI) JDUtilities.getGUI()).getFrame() : null, JDLocale.L("system.dialogs.update", "Updates available"), JDLocale.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size() + "", packages.size() + ""), 20, false, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);

                            if (ccd.result) {
                                Browser.download(JDUtilities.getResourceFile("webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");
                                // createDLCBackup();
                                JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                                logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                                System.exit(0);
                            }
                        } catch (HeadlessException e) {

                            e.printStackTrace();
                        }

                    }

                }

                progress.finalize();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
            }

            private void checkMessage() {
                File res = JDUtilities.getResourceFile("message.html");
                String hash = JDUtilities.getLocalHash(res);
                Browser.download(JDUtilities.getResourceFile("message.html"), "http://service.jdownloader.org/html/message.html");
                String hash2 = JDUtilities.getLocalHash(res);
                if (hash2 != null && !hash2.equals(hash)) {
                    String message = JDUtilities.getLocalFile(res);
                    if (message != null && message.trim().length() > 0) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.L("sys.warning.newMessage", "New Systemmessage"), message, 45, false, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_STOP_COUNTDOWN);
                        if (!ccd.result) {
                            res.delete();
                            res.deleteOnExit();
                        }
                    }
                }

            }

            // private void createDLCBackup() {
            // ProgressController p = new
            // ProgressController(JDLocale.L("init.backup.progress",
            // "Queue backup"), 3);
            //
            // File file = JDUtilities.getResourceFile("container/backup_" +
            // System.currentTimeMillis() + ".dlc");
            // p.increase(1);
            // JDUtilities.getController().saveDLC(file);
            // p.increase(2);
            // CFGConfig.getConfig("WEBUPDATE").setProperty("LAST_BACKUP",
            // file);
            // CFGConfig.getConfig("WEBUPDATE").save();
            // p.finalize(2000);
            //
            // }

        }.start();
    }

    public int getCid() {
        return cid;
    }

    void init() {
        CookieHandler.setDefault(null);

    }

    public JDController initController() {
        return new JDController();
    }

    public UIInterface initGUI(JDController controller) {

        UIInterface uiInterface = new SimpleGUI();
        controller.setUiInterface(uiInterface);
        controller.addControlListener(uiInterface);
        return uiInterface;
    }

    public void initPlugins() {
        logger.info("Lade Plugins");

        JDUtilities.setPluginForDecryptList(loadPluginForDecrypt());
        JDUtilities.setPluginForHostList(loadPluginForHost());
JDUtilities.setPluginForContainer(loadCPlugins());
        JDUtilities.setPluginOptionalList(loadPluginOptional());

        for (PluginOptional plg : JDUtilities.getPluginsOptional()) {
            try {
                if (JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + plg.getPluginName(), false) && !plg.initAddon()) {
                    logger.severe("Error loading Optional Plugin: FALSE");
                }
            } catch (Throwable e) {
                logger.severe("Error loading Optional Plugin: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public boolean installerWasVisible() {
        return installerVisible;
    }

    public Configuration loadConfiguration() {
        Object obj = JDUtilities.getDatabaseConnector().getData("jdownloaderconfig");

        if (obj == null) {
            File file = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
            if (file.exists()) {
                logger.info("Wrapping jdownloader.config");
                obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);
                System.out.println(obj.getClass().getName());
                JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", obj);
            }
        }

        if (obj != null && ((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null) {

            Configuration configuration = (Configuration) obj;
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "default"));

        } else {
            Configuration configuration = new Configuration();
            JDUtilities.setConfiguration(configuration);
            JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
            JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
            JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "default"));

            JDUtilities.getDatabaseConnector().saveConfiguration("jdownloaderconfig", JDUtilities.getConfiguration());
            installerVisible = true;
            try {
                splashScreen.finish();
            } catch (Exception e) {
            }
            SimpleGUI.setUIManager();
            Installer inst = new Installer();

            if (!inst.isAborted()) {

                File home = JDUtilities.getResourceFile(".");
                if (home.canWrite() && !JDUtilities.getResourceFile("noupdate.txt").exists()) {

                    JOptionPane.showMessageDialog(null, JDLocale.L("installer.welcome", "Welcome to jDownloader. Download missing files."));

                    Browser.download(new File(home, "webupdater.jar"), "http://service.jdownloader.org/update/webupdater.jar");

                    JDUtilities.saveConfig();
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, home.getAbsolutePath(), 0));
                    System.exit(0);

                }
                if (!home.canWrite()) {
                    logger.info("INSTALL abgebrochen");
                    JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.error.noWriteRights", "Error. You do not have permissions to write to " + home));
                    JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(1);
                }

            } else {
                logger.info("INSTALL abgebrochen2");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.abortInstallation", "Error. User aborted installation."));

                JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                System.exit(0);

            }
        }

        afterConfigIsLoaded();
        return JDUtilities.getConfiguration();
    }

    public void loadDownloadQueue() {
        if (!JDUtilities.getController().initDownloadLinks()) {
            File links = JDUtilities.getResourceFile("links.dat");

            if (links != null && links.exists()) {
                File newFile = new File(links.getAbsolutePath() + ".bup");
                newFile.delete();
                links.renameTo(newFile);
                JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("sys.warning.linklist.incompatible", "Linkliste inkompatibel. \r\nBackup angelegt."));
            }
        }

    }

    /**
     * Bilder werden dynamisch aus dem Homedir geladen.
     */
    public void loadImages() {
        ClassLoader cl = JDUtilities.getJDClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        File dir = JDUtilities.getResourceFile("jd/img/");

        String[] images = dir.list();
        if (images == null || images.length == 0) {
            logger.severe("Could not find the img directory");
            return;
        }
        for (String element : images) {
            if (element.toLowerCase().endsWith(".png") || element.toLowerCase().endsWith(".gif")) {
                File f = new File(element);

                logger.finer("Loaded image: " + f.getName().split("\\.")[0] + " from " + cl.getResource("jd/img/" + f.getName()));
                JDUtilities.addImage(f.getName().split("\\.")[0], toolkit.getImage(cl.getResource("jd/img/" + f.getName())));
            }

        }

    }

    public void loadModules() {
        logger.finer("create Module: Unrar");
        JDUtilities.getController().setUnrarModule(Unrar.getInstance());
        logger.finer("create Module: InfoFileWriter");

    }

    @SuppressWarnings("unchecked")
    public ArrayList<PluginsC> loadCPlugins() {
        ArrayList<PluginsC> plugins = new ArrayList<PluginsC>();
        ArrayList<String> containerTypes = new ArrayList<String>();
        containerTypes.add("D");
        containerTypes.add("R");
       // containerTypes.add("DLC");
        containerTypes.add("B");
        containerTypes.add("C");


        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        for (String cl : containerTypes) {

            logger.finer("Try to initialize " + cl);
            try {

                Class plgClass = jdClassLoader.loadClass("jd.plugins.a." + cl);
                if (plgClass == null) {
                    logger.info("PLUGIN NOT FOUND!");
                    continue;
                }
                Class[] classes = new Class[] {};
                Constructor con = plgClass.getConstructor(classes);

                plugins.add((PluginsC) con.newInstance(new Object[] {}));
                logger.finer("Successfully loaded " + cl);

            } catch (Throwable e) {
                logger.info("Plugin Exception!");
                e.printStackTrace();
            }
        }
        return plugins;

    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForDecrypt> loadPluginForDecrypt() {
        Vector<PluginForDecrypt> plugins = new Vector<PluginForDecrypt>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load Decrypt Plugins");
        Iterator iterator = Service.providers(PluginForDecrypt.class, jdClassLoader);

        while (iterator.hasNext()) {
            try {
                PluginForDecrypt p = (PluginForDecrypt) iterator.next();
                logger.finer("Load " + p);
                plugins.add(p);
            } catch (Exception e) {
                logger.info("caught");
                e.printStackTrace();
            }
        }
        return plugins;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForHost> loadPluginForHost() {
        Vector<PluginForHost> plugins = new Vector<PluginForHost>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load Host Plugins");
        Iterator iterator = Service.providers(PluginForHost.class, jdClassLoader);

        while (iterator.hasNext()) {
            try {
                PluginForHost p = (PluginForHost) iterator.next();
                logger.finer("Load " + p);
                plugins.add(p);
            } catch (Exception e) {
                logger.info("caught");
                e.printStackTrace();
            }
        }
        return plugins;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginOptional> loadPluginOptional() {

        Vector<PluginOptional> pluginsOptional = new Vector<PluginOptional>();
        class optionalPluginsVersions {
            public String name;
            public double version;

            public optionalPluginsVersions(String name, double version) {
                this.name = name;
                this.version = version;
            }

            public String toString() {
                return name;
            }
        }
        ArrayList<optionalPluginsVersions> optionalPluginsVersionsArray = new ArrayList<optionalPluginsVersions>();
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDTrayIcon", 1.6));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLightTray", 1.6));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("webinterface.JDWebinterface", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("schedule.Schedule", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDFolderWatch", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDShutdown", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDRemoteControl", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLowSpeed", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("HTTPLiveHeaderScripter", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("jdchat.JDChat", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("Newsfeeds", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDInfoFileWriter", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("StreamingShareTool", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("LangFileEditor", 1.5));

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        Double version = JDUtilities.getJavaVersion();
        for (optionalPluginsVersions cl : optionalPluginsVersionsArray) {
            if (version < cl.version) {
                logger.finer("Plugin " + cl + " requires Java Version " + cl.version + " your Version is: " + version);
                continue;
            }
            logger.finer("Try to initialize " + cl);
            try {

                Class plgClass = jdClassLoader.loadClass("jd.plugins.optional." + cl);
                if (plgClass == null) {
                    logger.info("PLUGIN NOT FOUND!");
                    continue;
                }
                Class[] classes = new Class[] {};
                Constructor con = plgClass.getConstructor(classes);

                try {

                    Method f = plgClass.getMethod("getAddonInterfaceVersion", new Class[] {});

                    int id = (Integer) f.invoke(null, new Object[] {});

                    if (id != PluginOptional.ADDON_INTERFACE_VERSION) {
                        logger.severe("Addon " + cl + " is outdated and incompatible. Please update(Packagemanager) :Addon:" + id + " : Interface: " + PluginOptional.ADDON_INTERFACE_VERSION);
                    } else {
                        pluginsOptional.add((PluginOptional) con.newInstance(new Object[] {}));
                        logger.finer("Successfully loaded " + cl);
                    }
                } catch (Exception e) {
                    logger.severe("Addon " + cl + " is outdated and incompatible. Please update(Packagemanager) :" + e.getLocalizedMessage());
                }

            } catch (Throwable e) {
                logger.info("Plugin Exception!");
                e.printStackTrace();
            }
        }

        return pluginsOptional;

    }

    public void removeFiles() {
        String[] remove = Regex.getLines(JDUtilities.getLocalFile(JDUtilities.getResourceFile("outdated.dat")));
        String homedir = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
        if (remove != null) {
            for (String file : remove) {
                if (file.length() == 0) continue;
                File delete = new File(homedir, file);
                if (!delete.toString().matches(".*?" + File.separator + "?\\.+" + File.separator + ".*?") && delete.toString().contains(homedir)) {
                    if (JDUtilities.removeDirectoryOrFile(delete)) logger.warning("Removed " + file);
                }
            }
        }
    }

}
