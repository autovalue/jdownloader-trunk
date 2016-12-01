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
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bs.to" }, urls = { "https?://(www\\.)?bs\\.to/(serie/[^/]+/\\d+/[^/]+(/[^/]+)?|out/\\d+)" })
public class BsTo extends PluginForDecrypt {

    public BsTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "https?://(www\\.)?bs\\.to/serie/[^/]+/\\d+/[^/]+/[^/]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (StringUtils.contains(parameter, "bs.to/out")) {
            this.br.setFollowRedirects(false);
            br.getPage(parameter);
            final String finallink = br.getRedirectLocation();
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String urlpart = new Regex(parameter, "(serie/.+)").getMatch(0);
        if (parameter.matches(TYPE_SINGLE)) {
            String finallink = br.getRegex("\"(https?[^<>\"]*?)\" target=\"_blank\"><span class=\"icon link_go\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<iframe\\s+[^>]+src\\s*=\\s*(\"|'|)(.*?)\\1").getMatch(1);
            }
            if (finallink == null) {
                throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
            } else if (finallink.contains("bs.to/out/")) {
                br.setFollowRedirects(false);
                br.getPage(finallink);
                finallink = br.getRedirectLocation();
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String fpName = null;
            final String[] links = br.getRegex("class=\"v\\-centered icon [^<>\"]+\"[\t\n\r ]+href=\"(" + urlpart + "/[^/]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink("http://bs.to/" + singleLink));
            }
        }

        return decryptedLinks;
    }
}
