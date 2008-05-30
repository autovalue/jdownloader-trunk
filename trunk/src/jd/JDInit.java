package jd;

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

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.InfoFileWriter;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
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

    private boolean installerVisible = false;

    private int cid = -1;
    private SplashScreen splashScreen;

    public JDInit() {
        this(null);
    }

    public JDInit(SplashScreen splashScreen) {
        this.splashScreen = splashScreen;
    }

    void init() {
        CookieHandler.setDefault(null);

    }

    /**
     * Bilder werden dynamisch aus dem Homedir geladen.
     */
    public void loadImages() {
        ClassLoader cl = JDUtilities.getJDClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        // try {
        // splashScreen.increase();
        // splashScreen.setText("Load images");
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        File dir = JDUtilities.getResourceFile("jd/img/");

        String[] images = dir.list();
        if (images == null || images.length == 0) {
            logger.severe("Could not find the img directory");
            return;
        }
        for (int i = 0; i < images.length; i++) {
            if (images[i].toLowerCase().endsWith(".png") || images[i].toLowerCase().endsWith(".gif")) {
                File f = new File(images[i]);
                // try {
                // splashScreen.increase(2);
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
                logger.finer("Loaded image: " + f.getName().split("\\.")[0] + " from " + cl.getResource("jd/img/" + f.getName()));
                JDUtilities.addImage(f.getName().split("\\.")[0], toolkit.getImage(cl.getResource("jd/img/" + f.getName())));
            }

        }

    }

    public Configuration loadConfiguration() {
        File fileInput = null;
        try {
            fileInput = JDUtilities.getResourceFile(JDUtilities.CONFIG_PATH);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        boolean allOK = true;
        try {

            if (fileInput != null && fileInput.exists()) {
                // try {
                // splashScreen.increase();
                // splashScreen.setText("Load Configuration");
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
                Object obj = JDUtilities.loadObject(null, fileInput, Configuration.saveAsXML);
                if (obj instanceof Configuration) {
                    Configuration configuration = (Configuration) obj;
                    JDUtilities.setConfiguration(configuration);
                    JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));
                    JDTheme.setTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_THEME, "default"));
                    JDSounds.setSoundTheme(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(JDSounds.PARAM_CURRENTTHEME, "default"));

                } else {
                    // log += "\r\n" + ("Configuration error: " + obj);
                    // log += "\r\n" + ("Konfigurationskonflikt. Lade Default
                    // einstellungen");
                    allOK = false;
                    if (JDUtilities.getConfiguration() == null) JDUtilities.getConfiguration().setDefaultValues();
                }
            } else {
                logger.info("no configuration loaded");
                logger.info("Konfigurationskonflikt. Lade Default einstellungen");

                allOK = false;
                if (JDUtilities.getConfiguration() == null) JDUtilities.getConfiguration().setDefaultValues();
            }
        } catch (Exception e) {
            logger.info("Konfigurationskonflikt. Lade Default einstellungen");
            e.printStackTrace();
            allOK = false;
            if (JDUtilities.getConfiguration() == null) JDUtilities.setConfiguration(new Configuration());
            JDUtilities.getConfiguration().setDefaultValues();
        }

        if (!allOK) {

            installerVisible = true;
            try {
                splashScreen.finish();
            } catch (Exception e) {
                // TODO: handle exception
            }
            SimpleGUI.setUIManager();
            Installer inst = new Installer();

            if (!inst.isAborted() && inst.getHomeDir() != null && inst.getDownloadDir() != null) {

                String newHome = inst.getHomeDir();
                logger.info("Home Dir: " + newHome);
                File homeDirectoryFile = new File(newHome);
                boolean createSuccessfull = true;
                if (!homeDirectoryFile.exists()) createSuccessfull = homeDirectoryFile.mkdirs();
                if (createSuccessfull && homeDirectoryFile.canWrite()) {
                    System.setProperty("jdhome", homeDirectoryFile.getAbsolutePath());
                    String dlDir = inst.getDownloadDir();

                    JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.welcome", "Welcome to jDownloader. Download missing files."));

                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, dlDir);

                    JDUtilities.download(new File(homeDirectoryFile, "webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                    JDUtilities.setHomeDirectory(homeDirectoryFile.getAbsolutePath());

                    JDUtilities.saveConfig();
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", "/restart", "/rt" + JDUtilities.RUNTYPE_LOCAL_JARED }, homeDirectoryFile.getAbsolutePath(), 0));
                    System.exit(0);

                }
                logger.info("INSTALL abgebrochen");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.error.noWriteRights", "Fehler. Bitte w�hle Pfade mit Schreibrechten!"));

                System.exit(1);
                inst.dispose();
            } else {
                logger.info("INSTALL abgebrochen2");
                JOptionPane.showMessageDialog(new JFrame(), JDLocale.L("installer.abortInstallation", "Fehler. Installation abgebrochen"));
                System.exit(0);
                inst.dispose();
            }
        }
        // try {
        // splashScreen.setText("Configuration loaded");
        // splashScreen.increase(5);
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        this.afterConfigIsLoaded();
        return JDUtilities.getConfiguration();
    }

    private void afterConfigIsLoaded() {

    }

    public JDController initController() {
        // try {
        // splashScreen.setText("init Controller");
        // splashScreen.increase(2);
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        return new JDController();
    }

    public UIInterface initGUI(JDController controller) {

        UIInterface uiInterface = new SimpleGUI();
        controller.setUiInterface(uiInterface);
        controller.addControlListener(uiInterface);
        return uiInterface;
    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForDecrypt> loadPluginForDecrypt() {
        Vector<PluginForDecrypt> plugins = new Vector<PluginForDecrypt>();
        // try {
        // splashScreen.setText("Load Plugins for Decrypt");
        // splashScreen.increase();
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        logger.finer("Load PLugins");
        Iterator iterator = Service.providers(PluginForDecrypt.class, jdClassLoader);
        // int c=0;
        while (iterator.hasNext()) {
            try {
                PluginForDecrypt p = (PluginForDecrypt) iterator.next();
                // try {
                //
                // if(c++%2==0)
                // {
                // splashScreen.setText(p.getPluginName());
                // splashScreen.increase();
                // }
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
                logger.info("Load " + p);
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
        // try {
        // splashScreen.setText("Load Plugins for Host");
        // splashScreen.increase();
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        Iterator iterator;
        logger.finer("Load PLugins");
        iterator = Service.providers(PluginForHost.class, jdClassLoader);
        // int c=0;
        while (iterator.hasNext()) {
            try {
                PluginForHost next = (PluginForHost) iterator.next();
                logger.finer("Load PLugins" + next);
                // try {
                // if(c++%2==0)
                // {
                // splashScreen.setText(next.getPluginName());
                // splashScreen.increase();
                // }
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
                PluginForHost p = next;

                plugins.add(p);
            } catch (Exception e) {
                logger.info("caught");
                e.printStackTrace();

            }
        }
        return plugins;

    }

    public void loadModules() {
        logger.finer("create Module: Unrar");
        JDUtilities.getController().setUnrarModule(Unrar.getInstance());
        logger.finer("create Module: InfoFileWriter");
        JDUtilities.getController().setInfoFileWriterModule(InfoFileWriter.getInstance());

    }

    @SuppressWarnings("unchecked")
    public Vector<PluginForContainer> loadPluginForContainer() {
        Vector<PluginForContainer> plugins = new Vector<PluginForContainer>();

        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();
        Iterator iterator;
        // try {
        // splashScreen.setText("Load Container Plugins");
        // splashScreen.increase();
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        logger.finer("Load PLugins");
        iterator = Service.providers(PluginForContainer.class, jdClassLoader);
        while (iterator.hasNext()) {
            try {
                PluginForContainer p = (PluginForContainer) iterator.next();
                // try {
                // splashScreen.setText(p.getPluginName());
                // splashScreen.increase();
                // } catch (Exception e) {
                // // TODO: handle exception
                // }
                plugins.add(p);
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("caught");

            }
        }
        return plugins;

    }

    @SuppressWarnings("unchecked")
    public HashMap<String, PluginOptional> loadPluginOptional() {
        // try {
        // splashScreen.setText("Load Optional Plugins");
        // splashScreen.increase();
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        HashMap<String, PluginOptional> pluginsOptional = new HashMap<String, PluginOptional>();
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
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDGetter", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLightTray", 1.6));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("webinterface.JDWebinterface", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("schedule.Schedule", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDFolderWatch", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDShutdown", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDRemoteControl", 1.5));
        optionalPluginsVersionsArray.add(new optionalPluginsVersions("JDLowSpeed", 1.5));
        JDClassLoader jdClassLoader = JDUtilities.getJDClassLoader();

        Double version = JDUtilities.getJavaVersion();
        Iterator<optionalPluginsVersions> iter = optionalPluginsVersionsArray.iterator();
        while (iter.hasNext()) {
            optionalPluginsVersions cl = (optionalPluginsVersions) iter.next();
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
                        logger.severe("Addon "+cl+" is outdated and incompatible. Please update(Packagemanager) :Addon:" + id + " : Interface: " + PluginOptional.ADDON_INTERFACE_VERSION);

                    } else {

                        PluginOptional p = (PluginOptional) con.newInstance(new Object[] {});
                        pluginsOptional.put(p.getPluginName(), p);
                        // try {
                        // splashScreen.setText(p.getPluginName());
                        // splashScreen.increase(2);
                        // } catch (Exception e) {
                        // // TODO: handle exception
                        // }
                        logger.finer("Successfull!. Loaded " + cl);
                    }
                } catch (Exception e) {
                    logger.severe("Addon "+cl+" is outdated and incompatible. Please update(Packagemanager) :" + e.getLocalizedMessage());

                }

            } catch (Throwable e) {
                logger.info("Plugin Exception!");
                e.printStackTrace();
            }
        }

        return pluginsOptional;

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

    public void initPlugins() {
        try {
            logger.info("Lade Plugins");
            // try {
            // splashScreen.setText("Load Plugins");
            // splashScreen.increase(3);
            // } catch (Exception e) {
            // // TODO: handle exception
            // }
            // JDController controller = JDUtilities.getController();
            JDUtilities.setPluginForDecryptList(this.loadPluginForDecrypt());
            JDUtilities.setPluginForHostList(this.loadPluginForHost());
            JDUtilities.setPluginForContainerList(this.loadPluginForContainer());
            try {
                JDUtilities.setPluginOptionalList(this.loadPluginOptional());
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            HashMap<String, PluginOptional> pluginsOptional = JDUtilities.getPluginsOptional();

            Iterator<String> iterator = pluginsOptional.keySet().iterator();
            String key;

            while (iterator.hasNext()) {
                key = iterator.next();
                PluginOptional plg = pluginsOptional.get(key);
                if (JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + plg.getPluginName(), false)) {
                    try {
                        if (!pluginsOptional.get(key).initAddon()) {
                            logger.severe("Error loading Optional Plugin: FALSE");
                        }

                    } catch (Throwable e2) {
                        logger.severe("Error loading Optional Plugin: " + e2.getMessage());
                        e2.printStackTrace();
                    }
                }
            }
        } catch (Throwable e2) {
            logger.severe("Error loading Optional Plugin: " + e2.getMessage());
            e2.printStackTrace();
        }
    }

    public void checkWebstartFile() {

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
        logger.info("hallo " + JDUtilities.getResourceFile("links.linkbackup"));
    }

    public void doWebupdate(final int oldCid, final boolean guiCall) {

        new Thread() {

            public void run() {
                ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title", "Webupdate"), 100);

                logger.finer("Init Webupdater");
                final WebUpdater updater = new WebUpdater(JDUtilities.getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_BETA", false) ? "http://jdbetaupdate.ath.cx" : null);

                updater.setCid(oldCid);
                logger.finer("Get available files");
                Vector<Vector<String>> files = updater.getAvailableFiles();
                // logger.info(files + "");
                updater.filterAvailableUpdates(files, JDUtilities.getResourceFile("."));
                // if(JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.USE_CAPTCHA_EXCHANGE_SERVER,
                // false)){
                // for (int i = files.size() - 1; i >= 0; i--) {
                //                  
                // // if
                // (files.get(i).get(0).startsWith("jd/captcha/methods/")&&files.get(i).get(0).endsWith("mth"))
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
                if (!guiCall && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    return;
                }

                if (files == null) {
                    logger.severe("Webupdater offline");
                    progress.finalize();
                    return;
                }

                int org;
                progress.setRange(org = files.size());
                logger.finer("Files found: " + files);

                logger.finer("init progressbar");
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title", "Update Check"));
                if (files != null) {

                    progress.setStatus(org - files.size());
                    logger.finer("FIles to update: " + files);
                    if (files.size() > 0) {
                        createQueueBackup();

                        logger.info("New Updates Available! " + files);

                        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                            JDUtilities.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                            JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                            logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                            System.exit(0);
                        } else {

                            try {
                                JHelpDialog d = new JHelpDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), "Update!", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + files.size() + " update(s) available. Start Webupdater now?" + "</font>");
                                d.getBtn3().setText("Cancel");
                                d.getBtn1().setText("Show changes");
                                d.getBtn2().setText(JDLocale.L("gui.dialogs.helpDialog.btn.ok", "Update now!"));
                                d.action1 = d.new Action() {
                                    public boolean doAction() {

                                        try {

                                            String update = JDUtilities.UTF8Decode(Plugin.getRequest(new URL(updater.getListPath().replaceAll("list.php", "") + "bin/updatemessage.html"), null, null, true).getHtmlCode());

                                            JDUtilities.getGUI().showHTMLDialog("Update Changes", update);
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }

                                        return true;
                                    }
                                };
                                d.showDialog();

                                if (d.getStatus() == JHelpDialog.STATUS_ANSWER_2) {
                                    JDUtilities.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloaderwebupdate.ath.cx");

                                    JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
                                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                                    System.exit(0);
                                }
                            } catch (HeadlessException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        }

                    }

                }

                progress.finalize();
                if (getCid() > 0 && getCid() != JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.CID, getCid());
                    JDUtilities.saveConfig();
                }
            }

        }.start();
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
            String lastLog = JDUtilities.UTF8Decode(JDUtilities.getLocalFile(JDUtilities.getResourceFile("updatemessage.html")));
            if (lastLog.trim().length() > 5) {
                if (splashScreen != null) splashScreen.finish();
                JDUtilities.getController().getUiInterface().showHTMLDialog("Update!", lastLog);
            }

        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_HASH, hash);
        JDUtilities.saveConfig();
    }

    public boolean installerWasVisible() {
        // TODO Auto-generated method stub
        return installerVisible;
    }

    public int getCid() {
        return cid;
    }

    public void removeFiles() {
        String[] remove = null;

       remove = new String[] { "jd/captcha/methods/filefactory.com","jd/captcha/methods/Stealth.to"  };
        if (remove != null) for (String file : remove) {

            if (JDUtilities.removeDirectoryOrFile(JDUtilities.getResourceFile(file))) {
                logger.warning("Removed " + file);
            }

        }

    }

    public void setupProxy() {
        // try {
        // splashScreen.increase(2);
        // splashScreen.setText("Setup Proxy");
        // } catch (Exception e) {
        // // TODO: handle exception
        // }
        System.setProperty("proxyHost", JDUtilities.getConfiguration().getStringProperty(Configuration.PROXY_HOST, ""));
        System.setProperty("proxyPort", JDUtilities.getConfiguration().getIntegerProperty(Configuration.PROXY_PORT, 80) + "");
        System.setProperty("http.proxyUser", JDUtilities.getConfiguration().getStringProperty(Configuration.PROXY_USER, ""));
        System.setProperty("http.proxyPassword", JDUtilities.getConfiguration().getStringProperty(Configuration.PROXY_PASS, ""));
        System.setProperty("proxySet", JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_PROXY, false) ? "true" : "false");
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_PROXY)) System.setProperty("proxyHost", "");
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_PROXY)) {
            logger.info("Use proxy:");
            logger.info("proxyHost: " + System.getProperty("proxyHost"));
            logger.info("proxyPort: " + System.getProperty("proxyPort"));
            logger.info("http.proxyUser: *******");
            logger.info("http.proxyPassword: ******");
        }

    }

}
