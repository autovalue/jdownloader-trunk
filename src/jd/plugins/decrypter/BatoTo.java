//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bato.to" }, urls = { "https?://bato\\.to/reader#[a-z0-9]+" }, flags = { 0 })
public class BatoTo extends PluginForDecrypt {

    public BatoTo(PluginWrapper wrapper) {
        super(wrapper);
        /* Prevent server response 503! */
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 3000);
    }

    public int getMaxConcurrentProcessingInstances() {
        /* Prevent server response 503! */
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        /* Login if possible */
        final PluginForHost host_plugin = JDUtilities.getPluginForHost("bato.to");
        final Account acc = AccountController.getInstance().getValidAccount(host_plugin);
        if (acc != null) {
            try {
                jd.plugins.hoster.BatoTo.login(this.br, acc, false);
            } catch (final Throwable e) {
            }
        }
        br.setAllowedResponseCodes(405);
        br.getPage(parameter.toString());// needed, sets cookie
        final String id = new Regex(parameter.toString(), "([a-z0-9]+)$").getMatch(0);
        final String url = "/areader?id=" + id + "&p=";
        // // enforcing one img per page because you can't always get all images displayed on one page.
        // br.setCookie("bato.to", "supress_webtoon", "t");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Referer", "http://bato.to/reader");
        br.getHeaders().put("Accept", "*/*");
        // Access page one
        br.getPage(url + 1);

        if (br.containsHTML("<div style=\"text-align:center;\"><img src=\"https?://[\\w\\.]*(?:batoto\\.net|bato\\.to)/images/404-Error\\.jpg\" alt=\"File not found\" /></div>|The page you were looking for is no longer available") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        } else if (this.br.getHttpConnection().getResponseCode() == 405) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        } else if (br.containsHTML(">This chapter has been removed due to infringement\\.<")) {
            decryptedLinks.add(this.createOfflinelink(url));
            return decryptedLinks;
        }

        String title_comic = br.getRegex("<li style=\"display: inline-block; margin-right: \\d+px;\"><a href=\"http://bato\\.to/comic/[^<>\"]+\">([^<>\"]*?)</a>").getMatch(0);
        // We get the title
        String title_tag = br.getRegex("value=\"https?://bato\\.to/reader#[a-z0-9]+\" selected=\"selected\">([^<>\"]*?)</option>").getMatch(0);
        // group
        String title_group = br.getRegex("<select name=\"group_select\"[^>]*>\\s*<option[^>]*>(.*?)</option>").getMatch(0);
        // final String
        if (title_tag == null) {
            /* Fallback if everything else fails! */
            title_tag = id;
        }

        title_tag = Encoding.htmlDecode(title_tag);
        title_tag = title_tag.replace(": ", " - ");
        final FilePackage fp = FilePackage.getInstance();
        // may as well set this globally. it used to belong inside 2 of the formatting if statements
        fp.setProperty("CLEANUP_NAME", false);
        fp.setName(Encoding.htmlDecode(title_comic).trim() + (title_comic != null ? " - " : "") + title_tag + (title_group != null ? " - " + title_group : ""));

        final String pages = br.getRegex(">page (\\d+)</option>\\s*</select>\\s*</li>").getMatch(0);
        if (pages == null) {
            // even though the cookie is set... they don't always respect this for small page count
            // http://www.batoto.net/read/_/249050/useful-good-for-nothing_ch1_by_suras-place
            // pages = br.getRegex(">page (\\d+)</option>\\s*</select>\\s*</li>").getMatch(0);
            // Temporary fix:
            final String imglist = br.getRegex("(<div style=\"text-align:center\\;\">.*?<img.*?<div)").getMatch(0);
            if (imglist != null) {
                logger.info("imglist: " + imglist);
                final String[] imgs = br.getRegex("<img src='(.*?)'").getColumn(0);
                for (final String img : imgs) {
                    final DownloadLink link = createDownloadlink(img);
                    String imgname = new Regex(img, "([^/]*)$").getMatch(0);
                    link.setFinalFileName(title_comic + " - " + title_tag + " - " + imgname);
                    link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                    link.setAvailable(true);
                    fp.add(link);
                    distribute(link);
                    decryptedLinks.add(link);
                }
                return decryptedLinks;
            }
        }
        if (pages == null) {
            logger.warning("Decrypter broken for: " + parameter + " @ pages");
            return null;
        }
        int numberOfPages = Integer.parseInt(pages);
        DecimalFormat df_page = new DecimalFormat("00");
        if (numberOfPages > 999) {
            df_page = new DecimalFormat("0000");
        } else if (numberOfPages > 99) {
            df_page = new DecimalFormat("000");
        }

        for (int i = 1; i <= numberOfPages; i++) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return decryptedLinks;
            }
            final String pageNumber = df_page.format(i);
            final DownloadLink link = createDownloadlink("http://bato.to/areader?id=" + id + "&p=" + pageNumber);
            final String fname_without_ext = fp.getName() + " - Page " + pageNumber;
            link.setProperty("fname_without_ext", fname_without_ext);
            link.setName(fname_without_ext);
            link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
            link.setAvailable(true);
            link.setContentUrl("http://bato.to/reader#" + id + "_" + pageNumber);
            fp.add(link);
            distribute(link);
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}