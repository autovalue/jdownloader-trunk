//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgsrc.ru" }, urls = { "http://(www\\.)?imgsrc\\.(ru|su|ro)/(main/passchk\\.php\\?(ad|id)=\\d+(&pwd=[a-z0-9]{32})?|main/(preword|pic_tape|warn|pic)\\.php\\?ad=\\d+(&pwd=[a-z0-9]{32})?|[^/]+/a?\\d+\\.html)" })
public class ImgSrcRu extends PluginForDecrypt {
    // dev notes
    // &pwd= is a md5 hash id once you've provided password for that album.
    public ImgSrcRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String                  password         = null;
    private String                  parameter        = null;
    private String                  username         = null;
    private String                  uid              = null;
    private String                  id               = null;
    private String                  aid              = null;
    private String                  pwd              = null;
    private boolean                 exaustedPassword = false;
    private boolean                 offline          = false;
    private PluginForHost           plugin           = null;
    private ArrayList<DownloadLink> decryptedLinks   = new ArrayList<DownloadLink>();
    private List<String>            passwords        = null;

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    private Browser prepBrowser(Browser prepBr, Boolean neu) {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("imgsrc.ru");
            if (plugin == null) {
                throw new IllegalStateException("imgsrc.ru hoster plugin not found!");
            }
            // set cross browser support
            ((jd.plugins.hoster.ImgSrcRu) plugin).setBrowser(br);
        }
        return ((jd.plugins.hoster.ImgSrcRu) plugin).prepBrowser(prepBr, neu);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final List<String> passwords = getPreSetPasswords();
        if (param.getDecrypterPassword() != null && !passwords.contains(param.getDecrypterPassword())) {
            passwords.add(param.getDecrypterPassword());
        }
        final String lastPass = this.getPluginConfig().getStringProperty("lastusedpassword");
        this.getPluginConfig().removeProperty("lastusedpassword");
        if (lastPass != null && !passwords.contains(lastPass)) {
            passwords.add(lastPass);
        }
        this.passwords = passwords;
        long startTime = System.currentTimeMillis();
        parameter = param.toString().replaceAll("https?://(www\\.)?imgsrc\\.(ru|ro|su)/", "http://imgsrc.ru/");
        prepBrowser(br, false);
        try {
            // best to get the original parameter, as the page could contain blocks due to forward or password
            if (!getPage(parameter, param)) {
                if (offline || exaustedPassword) {
                    decryptedLinks.add(createOfflinelink(parameter));
                    return decryptedLinks;
                } else {
                    return null;
                }
            }
            if (br.getURL().contains("http://imgsrc.ru/main/search.php")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            username = br.getRegex(">more photos from (.*?)</a>").getMatch(0);
            if (username == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String fpName = br.getRegex("from '<strong>([^\r\n]+)</strong>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?)(\\s*@\\s*iMGSRC.RU)?</title>").getMatch(0);
                if (fpName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
            aid = new Regex(parameter, "ad=(\\d+)").getMatch(0);
            if (aid == null) {
                aid = new Regex(parameter, "/a(\\d+)\\.html").getMatch(0);
            }
            uid = new Regex(parameter, "id=(\\d+)").getMatch(0);
            if (uid == null) {
                uid = new Regex(parameter, "/(\\d+)\\.html").getMatch(0);
            }
            if (uid == null && aid == null) {
                logger.warning("We could not find the UID, Please report this issue to JDownloader Development Team.");
                return null;
            }
            // We need to make sure we are on page 1 otherwise we could miss pages.
            // but this also makes things look tidy, making all parameters the same format
            if (aid != null) {
                id = "a" + aid;
            } else {
                id = uid;
            }
            parameter = "http://imgsrc.ru/" + username + "/" + id + ".html";
            param.setCryptedUrl(parameter);
            if (!br.getURL().matches(parameter + ".*?")) {
                if (!getPage(parameter, param)) {
                    if (offline || exaustedPassword) {
                        return decryptedLinks;
                    } else {
                        return null;
                    }
                }
            }
            String name = Encoding.htmlDecode(username.trim()) + " @ " + Encoding.htmlDecode(fpName.trim());
            FilePackage fp = FilePackage.getInstance();
            fp.setProperty("ALLOW_MERGE", true);
            fp.setName(Encoding.htmlDecode(name.trim()));
            parsePage(param);
            parseNextPage(param);
            for (DownloadLink link : decryptedLinks) {
                if (username != null) {
                    link.setProperty("username", username.trim());
                }
                if (fpName != null) {
                    link.setProperty("gallery", fpName.trim());
                }
            }
            fp.addLinks(decryptedLinks);
            if (decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } catch (Exception e) {
            if (!offline) {
                throw e;
            } else {
                logger.info("Link offline: " + parameter);
            }
        } finally {
            logger.info("Time to decrypt : " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds. Returning " + decryptedLinks.size() + " DownloadLinks for " + parameter);
        }
        return decryptedLinks;
    }

    private void parsePage(CryptedLink param) {
        ArrayList<String> imgs = new ArrayList<String>();
        // first link = album uid (uaid), these uid's are not transferable to picture ids (upid). But once you are past album page
        // br.getURL() is the correct upid.
        if (br.getURL().contains("/" + id)) {
            String currentID = br.getRegex("<img class=(cur|big) src=('|\")?https?://[^/]*(?:imgsrc\\.ru|icdn\\.ru)/[a-z]/" + username + "/\\d+/(\\d+)").getMatch(2);
            if (currentID == null) {
                currentID = br.getRegex("/abuse\\.php\\?id=(\\d+)").getMatch(0);
            }
            if (currentID != null) {
                currentID = "/" + username + "/" + currentID + ".html";
                if (pwd != null) {
                    currentID += "?pwd=" + pwd;
                }
                imgs.add(currentID);
            } else {
                logger.warning("ERROR parsePage");
                return;
            }
        } else {
            imgs.add(br.getURL().replaceFirst("https?://imgsrc.ru", ""));
        }
        String[] links = br.getRegex("<a href='(/" + username + "/\\d+\\.html(\\?pwd=[a-z0-9]{32})?)#bp'>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Possible plugin error: Please confirm in your webbrowser that this album " + parameter + " contains more than one image. If it does please report this issue to JDownloader Development Team.");
        }
        if (links != null && links.length != 0) {
            imgs.addAll(Arrays.asList(links));
        }
        if (imgs.size() != 0) {
            final String currentLink = br.getURL();
            for (String dl : imgs) {
                String upid = new Regex(dl, "/(\\d+)\\.html").getMatch(0);
                final DownloadLink img = createDownloadlink("http://decryptedimgsrc.ru" + dl);
                img.setProperty("Referer", currentLink);
                img.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPEG);
                img.setFinalFileName(upid);
                img.setAvailable(true);
                if (password != null) {
                    img.setProperty("pass", password);
                }
                decryptedLinks.add(img);
            }
        }
    }

    private boolean parseNextPage(CryptedLink param) throws Exception {
        String nextPage = br.getRegex("<a [^>]*href=\\s*(\"|'|)?(/" + username + "/\\d+\\.html(?:\\?pwd=[a-z0-9]{32})?)\\1>(▶|&#9654;|&#9658;)</a>").getMatch(1);
        if (nextPage != null) {
            if (!getPage(nextPage, param)) {
                return false;
            }
            parsePage(param);
            parseNextPage(param);
            return true;
        }
        return false;
    }

    private boolean getPage(String url, CryptedLink param) throws Exception {
        if (url == null || parameter == null) {
            return false;
        }
        if (pwd != null && !url.matches(".+?pwd=[a-z0-9]{32}")) {
            url += "?pwd=" + pwd;
        }
        boolean failed = false;
        final int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            try {
                if (isAbort()) {
                    throw new DecrypterException("Task Aborted");
                }
            } catch (final Throwable e) {
            }
            if (failed) {
                long meep = new Random().nextInt(4) * 1000;
                Thread.sleep(meep);
                failed = false;
            }
            try {
                br.getPage(url);
                if (br.containsHTML(">This album has not been checked by the moderators yet\\.|<u>Proceed at your own risk</u>")) {
                    // /main/passcheck.php?ad=\d+ links can not br.getURL + "?warned=yeah"
                    // lets look for the link
                    final String yeah = br.getRegex("/[^/]+/a\\d+\\.html\\?warned=yeah").getMatch(-1);
                    if (yeah != null) {
                        br.getPage(yeah);
                    } else {
                        // fail over
                        br.getPage(br.getURL() + "?warned=yeah");
                    }
                }
                // needs to be before password
                if (br.containsHTML("Continue to album >>")) {
                    String newLink = br.getRegex("\\((\"|')right\\1,function\\(\\) \\{window\\.location=('|\")(http://imgsrc\\.ru/[^<>\"\\'/]+/[a-z0-9]+\\.html((\\?pwd=)?(\\?pwd=[a-z0-9]{32})?)?)\\2").getMatch(2);
                    if (newLink == null) {
                        /* This is also possible: "/blablabla/[0-9]+.html?pwd=&" */
                        newLink = br.getRegex("href=(/[^<>\"]+\\?pwd=[^<>\"/]*?)><br><br>Continue to album >></a>").getMatch(0);
                    }
                    if (newLink == null) {
                        logger.warning("Couldn't process Album forward: " + parameter);
                        return false;
                    }
                    br.getPage(newLink);
                }
                if (br.containsHTML(">Album owner has protected his work from unauthorized access") || br.containsHTML("enter password to continue:")) {
                    Form pwForm = br.getFormbyProperty("name", "passchk");
                    if (pwForm == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return false;
                    }
                    if (passwords.size() > 0) {
                        password = passwords.remove(0);
                        } else {
                                password = getUserInput("Enter password for link: " + param.getCryptedUrl(), param);
                                if (password == null || password.equals("")) {
                                    logger.info("User aborted/entered blank password");
                                    exaustedPassword = true;
                                    return false;
                                }
                            }
                    pwForm.put("pwd", Encoding.urlEncode(password));
                    br.submitForm(pwForm);
                    pwForm = br.getFormbyProperty("name", "passchk");
                    if (pwForm != null) {
                        // nullify wrong storable to prevent retry loop of the same passwd multiple times.
                        password = null;
                        failed = true;
                        if (i == repeat) {
                            // using 'i' is probably not a good idea, as we could have had connection errors!
                            logger.warning("Exausted Password try : " + parameter);
                            exaustedPassword = true;
                            return false;
                        } else {
                            continue;
                        }
                    }
                    this.getPluginConfig().setProperty("lastusedpassword", password);
                    pwd = br.getRegex("\\?pwd=([a-z0-9]{32})").getMatch(0);
                }
                if (br.getURL().equals("http://imgsrc.ru/")) {
                    offline = true;
                    return false;
                }
                if (br.getURL().contains(url) || !failed) {
                    // because one page grab could have multiple steps, you can not break after each if statement
                    break;
                }
            } catch (final BrowserException e) {
                if (br.getHttpConnection().getResponseCode() == 410) {
                    offline = true;
                    return false;
                }
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted retry getPage count");
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}