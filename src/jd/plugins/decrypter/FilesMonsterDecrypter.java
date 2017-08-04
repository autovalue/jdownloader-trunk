//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Iterator;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "https?://(?:www\\.)?filesmonster\\.com/(?:download\\.php\\?id=[A-Za-z0-9_-]+|player/v\\d+/video/[A-Za-z0-9_-]+|dl/.*?/free/(?:[^\\s<>/]*/)*)" })
public class FilesMonsterDecrypter extends PluginForDecrypt {
    public FilesMonsterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  FILENAMEREGEX            = "\">File name:</td>[\t\n\r ]+<[^<>]+>(.*?)</td>";
    public static final String  FILESIZEREGEX            = "\">File size:</td>[\t\n\r ]+<[^<>]+>(.*?)</td>";
    private static final String ADDLINKSACCOUNTDEPENDANT = "ADDLINKSACCOUNTDEPENDANT";
    private static final String TYPE_EMBEDDED            = ".+/player/v3/video/.+";

    /**
     * TODO: Seems like some urls only have a free download option available if a certain Referer is present e.g.
     * https://board.jdownloader.org/showpost.php?p=343469&postcount=6
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String FAILED = null;
        final boolean onlyAddNeededLinks = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(ADDLINKSACCOUNTDEPENDANT, false);
        boolean addFree = true;
        boolean addPremium = true;
        if (onlyAddNeededLinks) {
            boolean accAvailable = false;
            final ArrayList<Account> accs = new ArrayList<Account>();
            try {
                // multihoster account!
                final ArrayList<Account> mHosterAccs = (ArrayList<Account>) AccountController.getInstance().getMultiHostAccounts("filesmonster.com");
                if (mHosterAccs != null && mHosterAccs.size() != 0) {
                    accs.addAll(mHosterAccs);
                }
            } catch (final Throwable t) {
            }
            // traditional account
            final ArrayList<Account> hoster = AccountController.getInstance().getAllAccounts("filesmonster.com");
            if (hoster != null && hoster.size() != 0) {
                accs.addAll(hoster);
            }
            if (accs != null && accs.size() != 0) {
                Iterator<Account> it = accs.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        accAvailable = true;
                        break;
                    }
                }
            }
            if (accAvailable) {
                addPremium = true;
                addFree = false;
            } else {
                addPremium = false;
                addFree = true;
            }
        }
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        final String parameter;
        if (param.toString().matches(TYPE_EMBEDDED)) {
            final String url_id = new Regex(param.toString(), "/([^/]+)$").getMatch(0);
            parameter = String.format("https://%s/download.php?id=%s", this.getHost(), url_id);
        } else {
            parameter = param.toString();
        }
        String protocol = new Regex(parameter, "(https?)://").getMatch(0);
        String browserReferrer = getBrowserReferrer();
        if (browserReferrer != null) {
            // we only need to set the domain.. not the url...
            browserReferrer = new Regex(browserReferrer, "https?://[^/]+/").getMatch(-1);
            br.setCurrentURL(browserReferrer);
        }
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.getPage(parameter);
        final String title = jd.plugins.hoster.FilesMonsterCom.getLongTitle(this.br);
        if (jd.plugins.hoster.FilesMonsterCom.isOffline(this.br)) {
            final DownloadLink finalOne = createDownloadlink(parameter.replace("filesmonster.com", "filesmonsterdecrypted.com"));
            finalOne.setAvailable(false);
            finalOne.setProperty("offline", true);
            finalOne.setName(new Regex(parameter, "download\\.php\\?id=(.+)").getMatch(0));
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        String fname = br.getRegex(FILENAMEREGEX).getMatch(0);
        String fsize = br.getRegex(FILESIZEREGEX).getMatch(0);
        String[] decryptedStuff = null;
        final String postThat = br.getRegex("\"(/dl/.*?)\"").getMatch(0);
        if (postThat != null) {
            br.postPage(postThat, "");
            final String findOtherLinks = br.getRegex("'(/dl/rft/.*?)\\'").getMatch(0);
            if (findOtherLinks != null) {
                br.getPage(findOtherLinks);
                decryptedStuff = br.getRegex("\\{(.*?)\\}").getColumn(0);
            }
        } else {
            FAILED = "Limit reached";
        }
        if (br.containsHTML(">You need Premium membership to download files larger than")) {
            FAILED = "There are no free downloadlinks";
        }
        if (addFree) {
            if (FAILED == null) {
                final String theImportantPartOfTheMainLink = jd.plugins.hoster.FilesMonsterCom.getMainLinkID(parameter);
                for (String fileInfo : decryptedStuff) {
                    String filename = new Regex(fileInfo, "\"name\":\"(.*?)\"").getMatch(0);
                    String filesize = new Regex(fileInfo, "\"size\":(\")?(\\d+)").getMatch(1);
                    String filelinkPart = new Regex(fileInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
                    if (filename == null || filesize == null || filelinkPart == null || filename.length() == 0 || filesize.length() == 0 || filelinkPart.length() == 0) {
                        logger.warning("FilesMonsterDecrypter failed while decrypting link:" + parameter);
                        return null;
                    }
                    String dllink = protocol + "://filesmonsterdecrypted.com/dl/" + theImportantPartOfTheMainLink + "/free/2/" + filelinkPart;
                    final DownloadLink finalOne = createDownloadlink(dllink);
                    // name here can be unicode....
                    finalOne.setFinalFileName(Encoding.htmlDecode(Encoding.unicodeDecode(filename)));
                    finalOne.setDownloadSize(Integer.parseInt(filesize));
                    finalOne.setAvailable(true);
                    // for this to be used in hoster plugin it needs non escaped name. see: getNewTemporaryLink
                    finalOne.setProperty("origfilename", filename);
                    finalOne.setProperty("origsize", filesize);
                    finalOne.setProperty("mainlink", parameter);
                    if (title != null) {
                        finalOne.setComment(title);
                    }
                    decryptedLinks.add(finalOne);
                }
                if (decryptedStuff == null || decryptedStuff.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            } else {
                logger.info("Failed to get free links because: " + FAILED);
            }
        }
        if (addPremium || FAILED != null) {
            final DownloadLink thebigone = createDownloadlink(parameter.replace("filesmonster.com", "filesmonsterdecrypted.com"));
            if (fname != null && fsize != null) {
                thebigone.setFinalFileName(Encoding.htmlDecode(fname.trim()));
                thebigone.setDownloadSize(SizeFormatter.getSize(fsize));
                thebigone.setAvailable(true);
            }
            thebigone.setProperty("PREMIUMONLY", true);
            if (title != null) {
                thebigone.setComment(title);
            }
            decryptedLinks.add(thebigone);
        }
        /** All those links belong to the same file so lets make a package */
        if (fname != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fname.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}