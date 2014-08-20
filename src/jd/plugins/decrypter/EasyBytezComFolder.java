//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "easybytez.com" }, urls = { "http://(www\\.)?easybytez.com/users/[^<>\"\\?\\&]+" }, flags = { 0 })
public class EasyBytezComFolder extends PluginForDecrypt {

    public EasyBytezComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setCookie("http://easybytez.com", "", "");
        br.getPage(parameter);
        if (br.containsHTML(">\\s*Guest access not possible!\\s*<")) {
            // we only login when required!
            final boolean logged_in = getLogin();
            if (!logged_in) {
                logger.info("Guest access not possible!");
                logger.info("Login failed or no accounts active/existing -> Continuing without account");
                return decryptedLinks;
            }
        }
        int lastPage = 1;
        final String[] allPages = br.getRegex("<a href='\\?\\&amp;page=(\\d+)'").getColumn(0);
        if (allPages != null && allPages.length != 0) {
            for (final String aPage : allPages) {
                final int pp = Integer.parseInt(aPage);
                if (pp > lastPage) {
                    lastPage = pp;
                }
            }
        }
        for (int i = 1; i <= lastPage; i++) {
            br.getPage(parameter + "?&page=" + i);
            String[] links = br.getRegex("class=\"link\"><a href=\"(http://(www\\.)?easybytez\\.com/[a-z0-9]{12})\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("<td><a href=\"(http://(www\\.)?easybytez\\.com/[a-z0-9]{12})\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                if (br.containsHTML(">The selected folder contains the following files")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getLogin() throws Exception {
        final PluginForHost easybytezPlugin = JDUtilities.getPluginForHost("easybytez.com");
        final Account aa = AccountController.getInstance().getValidAccount(easybytezPlugin);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).setBrowser(this.br);
                boolean fresh = false;
                Object after = null;
                synchronized (jd.plugins.hoster.EasyBytezCom.ACCLOCK) {
                    Object before = aa.getProperty("cookies", null);
                    after = ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).login(aa, false);
                    fresh = before != after;
                    if (fresh) {
                        final String myAccount = "/?op=my_account";
                        if (br.getURL() == null) {
                            br.setFollowRedirects(true);
                            ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).getPage(((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).COOKIE_HOST.replaceFirst("https?://", ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).getProtocol()) + myAccount);
                        } else if (!br.getURL().contains(myAccount)) {
                            ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).getPage(myAccount);
                        }
                        ((jd.plugins.hoster.EasyBytezCom) easybytezPlugin).updateAccountInfo(aa, aa.getAccountInfo(), this.br);
                    }
                }
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid!");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}