//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de", "spon.de" }, urls = { "https?://(?:www\\.)?spiegel\\.de/(?:fotostrecke/[a-z0-9\\-]+\\d+(\\-\\d+)?\\.html|artikel/a\\-\\d+\\.html)", "https?://(?:www\\.)?spon\\.de/[A-Za-z0-9]+" })
public class SpiegelDe extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORED_FOTOSTRECKE         = Pattern.compile("https?://(?:www\\.)?spiegel\\.de/fotostrecke/[a-z0-9\\-]+\\.html", Pattern.CASE_INSENSITIVE);
    private static final String  PATTERN_SUPPORTED_FOTOSTRECKE_SINGLE = "https?://(?:www\\.)?spiegel\\.de/fotostrecke/[a-z0-9\\-]+\\d+\\-\\d+\\.html";

    private static final String  PATTERN_SUPPORTED_SPON_SHORT_URL     = ".+spon\\.de/[A-Za-z0-9]+";
    private static final String  PATTERN_SUPPORTED_REDIRECT_ARTICLE   = ".+/artikel/.+";

    // Patterns für Fotostrecken
    private static final Pattern PATTERN_IMG_URL                      = Pattern.compile("<a id=\"spFotostreckeControlImg\" href=\"(/fotostrecke/fotostrecke-\\d+-\\d+.html)\"><img src=\"(http://www.spiegel.de/img/.+?(\\.\\w+?))\"");
    private static final Pattern PATTERN_IMG_TITLE                    = Pattern.compile("<meta name=\"description\" content=\"(.+?)\" />");

    public SpiegelDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String url = cryptedLink.getCryptedUrl();
        if (new Regex(url, PATTERN_SUPPORTED_SPON_SHORT_URL).matches() || new Regex(url, PATTERN_SUPPORTED_REDIRECT_ARTICLE).matches()) {
            /* SpiegelOnline short urls (redirect urls) */
            br.setFollowRedirects(false);
            this.br.getPage(url);
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                /* Probably offline */
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
        } else {
            /* Picture galleries */
            br.setFollowRedirects(true);
            this.br.getPage(url);
            String title = br.getRegex(SpiegelDe.PATTERN_IMG_TITLE).getMatch(0);
            if (title == null) {
                logger.warning("Decrypter broken for link: " + cryptedLink.toString());
                return null;
            }
            title = Encoding.htmlDecode(title.trim());
            if (new Regex(cryptedLink.getCryptedUrl(), SpiegelDe.PATTERN_SUPPORTED_FOTOSTRECKE_SINGLE).matches()) {
                final String finallink = br.getRegex("<div class=\"biga\\-image\".*?<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + cryptedLink.toString());
                    return null;
                }
                final String finalname = title + ".jpg";
                final DownloadLink dlLink = this.createDownloadlink(finallink);
                dlLink.setProperty("decryptedfilename", finalname);
                dlLink.setFinalFileName(finalname);
                decryptedLinks.add(dlLink);
            } else if (new Regex(cryptedLink.getCryptedUrl(), SpiegelDe.PATTERN_SUPPORED_FOTOSTRECKE).matches()) {
                int count = 1;
                final FilePackage filePackage = FilePackage.getInstance();
                filePackage.setName(title.trim());
                String next = url;
                while (next != null) {
                    if (count > 1) {
                        br.getPage(next);
                    }
                    next = br.getRegex("<link rel=\"next\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
                    final String imgLink = br.getRegex("<div class=\"biga\\-image\".*?<img src=\"(http://[^<>\"]*?)\"").getMatch(0);
                    if (imgLink == null) {
                        break;
                    }
                    final String ending = getFileNameExtensionFromString(imgLink);
                    if (imgLink != null) {
                        final String finalname = title + "-" + count + ending;
                        final DownloadLink dlLink = this.createDownloadlink(imgLink);
                        filePackage.add(dlLink);
                        dlLink.setProperty("decryptedfilename", finalname);
                        dlLink.setFinalFileName(title + "-" + count + ending);
                        dlLink.setAvailable(true);
                        decryptedLinks.add(dlLink);
                        count++;
                    }
                    if (decryptedLinks.size() == 0) {
                        logger.warning("Decrypter broken for link: " + cryptedLink.toString());
                        return null;
                    }
                }
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}