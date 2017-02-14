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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDHexUtils;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "https?://(?:www\\.)?mixcloud\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_%]+/" })
public class MxCloudCom extends antiDDoSForDecrypt {

    public MxCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private final boolean attemptToDownloadOriginal = false;

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> tempLinks = new ArrayList<String>();
        final String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches("https?://(?:www\\.)?mixcloud\\.com/((developers|categories|media|competitions|tag|discover)/.+|[\\w\\-]+/(playlists|activity|followers|following|listens|favourites).+)")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        getPage(parameter);
        if (br.getRedirectLocation() != null) {
            logger.info("Unsupported or offline link: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("<title>404 Error page|class=\"message\\-404\"|class=\"record\\-error record\\-404") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        String theName = br.getRegex("class=\"cloudcast\\-name\" itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (theName == null) {
            theName = br.getRegex("data-resourcelinktext=\"(.*?)\"").getMatch(0);
            if (theName == null) {
                theName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                if (theName == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
        }

        final String playInfo = br.getRegex("m\\-play\\-info=\"([^\"]+)\"").getMatch(0);
        if (playInfo == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String url_mp3_preview = br.getRegex("\"(https?://[A-Za-z0-9]+\\.mixcloud\\.com/previews/[^<>\"]*?\\.mp3)\"").getMatch(0);
        if (url_mp3_preview != null && attemptToDownloadOriginal) {
            /* 2016-10-24: Original-file download not possible anymore(?!) */
            final Regex originalinfo = new Regex(url_mp3_preview, "(https?://[A-Za-z0-9]+\\.mixcloud\\.com)/previews/([^<>\"]+\\.mp3)");
            final String previewLinkpart = originalinfo.getMatch(1);
            if (previewLinkpart != null) {
                /* 2016-05-01: It seems like it is not possibly anymore to download the original uploaded file (mp3) :( */
                /* TODO: Find a way to get that server dynamically */
                final String mp3link = "https://stream19.mixcloud.com/c/originals/" + previewLinkpart;
                tempLinks.add(mp3link);
            }
            tempLinks.add(url_mp3_preview);
        }

        String result = null;
        try {
            /*
             * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
             */
            result = decrypt(playInfo);
        } catch (final Throwable e) {
            return null;
        }

        final String[] links = new Regex(result, "\"(http.*?)\"").getColumn(0);
        if (links != null && links.length != 0) {
            for (final String temp : links) {
                tempLinks.add(temp);
            }
        }
        if (tempLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final HashMap<String, Long> alreadyFound = new HashMap<String, Long>();
        boolean streamFailed;
        br.setFollowRedirects(true);
        for (final String dl : tempLinks) {
            streamFailed = false;
            if (!dl.endsWith(".mp3") && !dl.endsWith(".m4a")) {
                continue;
            }
            URLConnectionAdapter con = null;
            final Browser br = this.br.cloneBrowser();
            final DownloadLink dlink = createDownloadlink("directhttp://" + dl);
            dlink.setFinalFileName(Encoding.htmlDecode(theName).trim() + new Regex(dl, "(\\..{3}$)").getMatch(0));
            /* Nicht alle Links im Array sets[] sind verfügbar. */
            try {
                try {
                    con = br.openGetConnection(dl);
                } catch (final Throwable e) {
                    streamFailed = true;
                }
                if (streamFailed || con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    continue;
                }
                if (alreadyFound.get(dlink.getName()) != null && alreadyFound.get(dlink.getName()) == con.getLongContentLength()) {
                    continue;
                } else {
                    alreadyFound.put(dlink.getName(), con.getLongContentLength());
                    dlink.setAvailable(true);
                    dlink.setDownloadSize(con.getLongContentLength());
                    decryptedLinks.add(dlink);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(theName));
        fp.addLinks(decryptedLinks);
        if ((decryptedLinks == null || decryptedLinks.size() == 0) && links != null && links.length > 0) {
            logger.info("Found urls but all of them were offline");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String decrypt(String e) {
        final byte[] key = JDHexUtils.getByteArray(JDHexUtils.getHexString(Encoding.Base64Decode("cGxlYXNlZG9udGRvd25sb2Fkb3VybXVzaWN0aGVhcnRpc3Rzd29udGdldHBhaWQ=")));
        final byte[] enc = jd.crypt.Base64.decode(e);
        byte[] plain = new byte[enc.length];
        int count = enc.length;
        int i = 0;
        int j = 0;
        while (i < count) {
            if (j > key.length - 1) {
                j = 0;
            }
            plain[i] = (byte) (0xff & (enc[i] ^ key[j]));
            i++;
            j++;
        }
        return new String(plain);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}