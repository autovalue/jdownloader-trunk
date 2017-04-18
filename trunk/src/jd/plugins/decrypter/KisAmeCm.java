//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.KissanimeToConfig;
import org.jdownloader.plugins.components.google.GoogleVideoRefresh;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kissanime.to", "kissasian.com", "kisscartoon.me" }, urls = { "https?://(?:www\\.)?kissanime\\.(?:com|to|ru)/anime/[a-zA-Z0-9\\-\\_]+/[a-zA-Z0-9\\-\\_]+(?:\\?id=\\d+)?", "http://kissasian\\.com/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?", "https?://kisscartoon\\.(?:me|io)/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?" })
public class KisAmeCm extends antiDDoSForDecrypt implements GoogleVideoRefresh {
    public KisAmeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return KissanimeToConfig.class;
    }

    private enum HostType {
        KISS_ANIME,
        KISS_ASIAN,
        KISS_CARTOON,
        KISS_UNKNOWN;

        private static HostType parse(final String link) {
            if (StringUtils.containsIgnoreCase(link, "kissanime")) {
                return KISS_ANIME;
            } else if (StringUtils.containsIgnoreCase(link, "kissasian")) {
                return KISS_ASIAN;
            } else if (StringUtils.containsIgnoreCase(link, "kisscartoon")) {
                return KISS_CARTOON;
            } else {
                return KISS_UNKNOWN;
            }
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HostType hostType = HostType.parse(param.toString());
        final String parameter;
        if (hostType == HostType.KISS_CARTOON) {
            parameter = param.toString().replace("kisscartoon.me", "kisscartoon.io");
        } else {
            parameter = param.toString();
        }
        final KissanimeToConfig pc = PluginJsonConfig.get(KissanimeToConfig.class);
        if (pc == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "User has no config!");
        }
        final boolean grabBEST = pc.isGrabBestVideoVersionEnabled();
        final boolean grab1080p = pc.isGrab1080pVideoEnabled();
        final boolean grab720p = pc.isGrab720pVideoEnabled();
        final boolean grab480p = pc.isGrab480pVideoEnabled();
        final boolean grab360p = pc.isGrab360pVideoEnabled();

        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final HashMap<String, DownloadLink> qualities = new HashMap<String, DownloadLink>();
        handleHumanCheck(this.br);
        String title;
        if (hostType == HostType.KISS_CARTOON) {
            title = br.getRegex("<title>\\s*(.*?)\\s*Online Free").getMatch(0);
        } else {
            title = br.getRegex("<title>\\s*(.*?)\\s*- Watch\\s*\\1[^<]*</title>").getMatch(0);
        }
        if (title == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        title = title.replaceAll("\\s+", " ");
        final String url_base = this.br.getURL();
        String[] mirrors = this.br.getRegex("(\\&s=[A-Za-z0-9\\-_]+)\"").getColumn(0);
        if (mirrors.length == 0) {
            mirrors = new String[] { "dummy" };
        }

        for (int mirror_number = 0; mirror_number <= mirrors.length - 1; mirror_number++) {
            if (mirror_number > 0) {
                final String mirror_param = mirrors[mirror_number];
                getPage(url_base + mirror_param);
            }
            // we have two things we need to base64decode
            final String[][] quals;
            if (hostType == HostType.KISS_CARTOON) {
                quals = getQualsCartoon(this.br, parameter);
            } else {
                quals = getQuals(this.br);
            }
            if (quals != null) {
                String skey = null;
                String iv = null;
                switch (hostType) {
                case KISS_ANIME:
                    skey = getSecretKeyAnime();
                    iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                    break;
                case KISS_ASIAN:
                    skey = getSecretKeyAsian();
                    iv = "32b812e9a1321ae0e84af660c4722b3a";
                    break;
                default:
                    break;
                }
                for (final String qual[] : quals) {
                    String decode = null;
                    switch (hostType) {
                    case KISS_ANIME:
                    case KISS_ASIAN:
                        decode = decodeSingleURL(qual[1], skey, iv);
                        break;
                    case KISS_CARTOON:
                        decode = qual[1];
                        break;
                    default:
                        break;
                    }
                    if (decode == null) {
                        continue;
                    }
                    final String quality = qual[2];
                    final DownloadLink dl = createDownloadlink(decode);
                    /* md5 of "kissanime.com" */
                    dl.setProperty("refresh_url_plugin", getHost());
                    dl.setProperty("source_url", parameter);
                    dl.setProperty("source_quality", quality);
                    dl.setFinalFileName(title + "-" + quality + ".mp4");
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    /* Best comes first --> Simply quit the loop if user wants best quality. */
                    if (grabBEST) {
                        decryptedLinks.add(dl);
                        break;
                    }
                    qualities.put(quality, dl);
                }
                if (!grabBEST) {
                    if (grab1080p && qualities.containsKey("1080p")) {
                        decryptedLinks.add(qualities.get("1080p"));
                    }
                    if (grab720p && qualities.containsKey("720p")) {
                        decryptedLinks.add(qualities.get("720p"));
                    }
                    if (grab480p && qualities.containsKey("480p")) {
                        decryptedLinks.add(qualities.get("480p"));
                    }
                    if (grab360p && qualities.containsKey("360p")) {
                        decryptedLinks.add(qualities.get("360p"));
                    }
                }
            } else {
                /* iframed.. seen openload.. but maybe others */
                final String link = br.getRegex("\\$\\('#divContentVideo'\\)\\.html\\('<iframe\\s+[^>]* src=\"(.*?)\"").getMatch(0);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }

            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[][] getQuals(final Browser br) {
        String[][] quals = null;
        final String qualityselection = br.getRegex("<select id=\"(?:selectQuality|slcQualix)\">.*?</select").getMatch(-1);
        if (qualityselection != null) {
            quals = new Regex(qualityselection, "<option [^>]*value\\s*=\\s*('|\"|)(.*?)\\1[^>]*>(\\d+p)").getMatches();
        }
        return quals;
    }

    @SuppressWarnings("unchecked")
    public String[][] getQualsCartoon(final Browser br, final String param) throws Exception {
        String fid = new Regex(param, "id=(.*)").getMatch(0);
        final Browser ajax = br.cloneBrowser();
        postPage(ajax, "https://kisscartoon.io/ajax/anime/load_episodes", "episode_id=" + Encoding.urlEncode(fid));
        String queryUrl = PluginJSonUtils.getJson(ajax, "value");
        ajax.getHeaders().put("Content-Type", "application/json");
        getPage(ajax, queryUrl + "&_=" + System.currentTimeMillis());
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        List<Object> playlist = (List<Object>) entries.get("playlist");
        List<Object> sources = (List<Object>) ((LinkedHashMap<String, Object>) playlist.get(0)).get("sources");
        String[][] quals = new String[sources.size()][3];
        for (int i = 0; i < sources.size(); i++) {
            quals[i][0] = "";
            String fileUrl = (String) ((LinkedHashMap<String, Object>) sources.get(i)).get("file");
            if (StringUtils.contains(fileUrl, "blogspot.com/")) {
                // this is redirect bullshit
                final Browser test = new Browser();
                test.setFollowRedirects(false);
                getPage(test, fileUrl);
                fileUrl = test.getRedirectLocation();
            }
            quals[i][1] = fileUrl;
            quals[i][2] = (String) ((LinkedHashMap<String, Object>) sources.get(i)).get("label");
        }
        return quals;
    }

    private String getSecretKeyAnime() {
        String match1 = br.getRegex("<script[^>]*>\\s*var.*?\\[\"([^\"]+)\",.*?CryptoJS.SHA256\\(").getMatch(0);
        String skey = "nhasasdbasdtene7230asb";
        if (match1 != null) {
            skey = Encoding.unescape(match1);
        }
        boolean match2 = br.getRegex("src=\"/Scripts/shal.js\">").matches();
        if (match2) {
            skey += "6n23ncasdln213";
        }
        boolean match3 = br.getRegex("src=\"/Scripts/file3.js\">").matches();
        if (match3) {
            skey = skey.replace("a", "c");
        }
        return skey;
    }

    private String getSecretKeyAsian() throws Exception {
        final Browser ajax = br.cloneBrowser();
        postPage(ajax, "/External/RSK", "krsk=665");
        String postData = ajax.toString();

        String skey = postData;
        String varName = br.getRegex("\\\\x67\\\\x65\\\\x74\\\\x53\\\\x65\\\\x63\\\\x72\\\\x65\\\\x74\\\\x4B\\\\x65\\\\x79\"];var ([^=]+)=\\$kissenc").getMatch(0);
        String match1 = br.getRegex("<script[^>]*>\\s*" + varName + " \\+= '([^']+)';\\s*</").getMatch(0);
        if (match1 != null) {
            skey += match1;
        }
        String[][] match2 = br.getRegex("var _x1 = '([^']+)'.*?x352\\('([^']+)',.*?'([^']+)';\\s*</").getMatches();
        if (match2.length != 0) {
            skey = match2[0][1] + "9c" + match2[0][0] + skey + match2[0][2];
        }
        String[][] match3 = br.getRegex("'k', 'l', 'm'];[^']+'([^']+)'[^']+'([^']+)';").getMatches();
        if (match3.length != 0) {
            skey = match3[0][0] + "uef" + skey + match3[0][1];
        }
        return skey;
    }

    public String decodeSingleURL(final String encodedString, final String skey, final String iv) {
        String decode = null;
        try {
            byte[] encodedArray = Base64.decode(encodedString);
            String hash = Hash.getSHA256(skey);
            // AES256
            byte[] byteKey = hexStringToByteArray(hash);
            byte[] byteIv = hexStringToByteArray(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(byteKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(byteIv);
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                byte[] byteResult = cipher.doFinal(encodedArray);
                decode = new String(byteResult, "UTF-8");
            } catch (final Exception e) {
                logger.info(e.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // decode:
            // http://kissasian.com/Play?key=AaKw5OKkuxsCX+g11bWAThZKjXqDGPQWhjJWnuT5dTGj1fd1cpjSu3IoG+BS9DrWjnEmbfIX6hzW21Wg+0x1x2xKpQsxMSf2Qq+Sj0x1x2xXplONQDkgERfg+oOe+8gdidU13A97bfhucXMOZpuH+qEyMsodmaW+HdcTG3FwUxF3b2QSmHTllXLU3LLVSvxWtiICNbHITdp9r9yaf6r9fVHXTvmGgwvQqC7Kn6A2VYb8FDelQLZfIcXr8xP9Gcjp2rZw9UTCZetxz6tPFXxUAOBTNfMirs90x1x2xNgCaH3CY2Dd0CnmDWTetRbccDcEwQ0gwGnXUrtsjcjnRykZZ53lFlyfsoWk8RmJc4QQKF58fRMuZNcB9My7lmIO5km6Y+sr0x1x2xGUJqlPB9YV1GH4PiiR4YS8XYwFg21p0x1x2x1VjNDsCKIVQSwCWw622f5Fh45xaWNzys0x1x2xqPuIeYiXHYReyUodf6hoWtfatwxeenjIV71W7JNfuVaxvpjasM0ahDd3QevYcZa3WJLDmMxKVvFmfXdGu0Mp647bmb4FPOP0x1x2xwQfOiFNWfl4d974FAztZyGzGMNxBDFMR4oqaVKCG+U0x1x2xP3bDAVQjW+1Ig9aIifmHzusAchhcVI8vsa9m2SarsZ4mjVzC3hikmvP6+ojddwd99fJP3tupbAL6sI3FDmf8O4ipGD5jMPUDeR+BvtD90lLA12cQXg0Oj+91gv4FR4JRC5oHybnjkFCUnSRSYiB7AGg5YrpxHq6WV7m2JuEWHlqUpNh+OCZEXriY+SMbNOK+LLobcA9KF10nSA3TarRRyMBPpLZquyeGkKyjxThfpIP1RYppeUJPP6Dcogfc00ESESAK0rGZELE6ahHwpB2eA==
            if (StringUtils.contains(decode, "kissasian.com/") || StringUtils.contains(decode, "blogspot.com/")) {
                // this is redirect bullshit
                final Browser test = br.cloneBrowser();
                test.setFollowRedirects(false);
                getPage(test, decode);
                // timeout:
                // Location: /Message/UnknownError?aspxerrorpath=/Play
                // <html><head><title>Object moved</title></head><body>
                // <h2>Object moved to <a href="/Message/UnknownError?aspxerrorpath=/Play">here</a>.</h2>
                // </body></html>
                // 503:
                decode = test.getRedirectLocation();
            }
        } catch (final Throwable e) {
        }
        return decode;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        byte[] bytes = new byte[hexString.length() / 2];

        for (int i = 0; i < hexString.length(); i += 2) {
            String sub = hexString.substring(i, i + 2);
            Integer intVal = Integer.parseInt(sub, 16);
            bytes[i / 2] = intVal.byteValue();
        }
        return bytes;
    }

    public static boolean isOffline(final Browser br) {
        return br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404;
    }

    private void handleHumanCheck(final Browser br) throws Exception {
        int retries = 5;
        while (retries-- > 0) {
            if (isAbort()) {
                throw new InterruptedException("Aborted");
            }
            final Form ruh = br.getFormbyAction("/Special/AreYouHuman");
            // recaptchav2 event can happen here
            if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || ruh != null) {
                if (ruh == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                if (ruh.containsHTML("g-recaptcha")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    ruh.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(br, ruh);
                } else if (ruh.containsHTML("solvemedia\\.com/papi/")) {
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode("solvemedia", cf, param);
                    if ("".equals(code)) {
                        // refresh (f5) button returns "", but so does a empty response by the user (send button)
                        continue;
                    }
                    final String chid = sm.getChallenge(code);
                    ruh.put("adcopy_response", Encoding.urlEncode(code));
                    ruh.put("adcopy_challenge", Encoding.urlEncode(chid));
                    submitForm(br, ruh);
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        continue;
                    }
                    break;
                } else {
                    // unsupported captcha type?
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
            } else {
                break;
            }
        }
        if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || br.getFormbyAction("/Special/AreYouHuman") != null) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
    }

    public String refreshVideoDirectUrl(final DownloadLink dl, final Browser br) throws Exception {
        String directlink = null;
        final String source_url = dl.getStringProperty("source_url", null);
        final String source_quality = dl.getStringProperty("source_quality", null);
        if (source_url == null || source_quality == null) {
            return null;
        }
        getPage(br, source_url);
        if (isOffline(br)) {
            return null;
        }
        handleHumanCheck(br);
        HostType hostType = HostType.parse(source_url);
        /* Find new directlink for original quality */
        final String[][] quals;
        if (hostType == HostType.KISS_CARTOON) {
            quals = getQualsCartoon(this.br, source_url);
        } else {
            quals = getQuals(this.br);
        }
        if (quals != null) {
            String skey = null;
            String iv = null;
            switch (hostType) {
            case KISS_ANIME:
                skey = getSecretKeyAnime();
                iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                break;
            case KISS_ASIAN:
                skey = getSecretKeyAsian();
                iv = "32b812e9a1321ae0e84af660c4722b3a";
                break;
            default:
                break;
            }
            for (final String qual[] : quals) {
                final String quality = qual[2];
                if (!quality.equalsIgnoreCase(source_quality)) {
                    continue;
                }
                switch (hostType) {
                case KISS_ANIME:
                case KISS_ASIAN:
                    directlink = decodeSingleURL(qual[1], skey, iv);
                    break;
                case KISS_CARTOON:
                    directlink = qual[1];
                    break;
                default:
                    break;
                }

                break;
            }
        }
        return directlink;
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    private void test() throws Exception {
        // JavascriptHtmlUnit.getHtmlAsXml(br, br.getURL());
        Browser br2 = br.cloneBrowser();
        getPage(br2, "/Scripts/kissenc.min.js");
        final String k1 = br2.toString();
        final String k2 = br2.getRegex("eval(.*?);$").getMatch(0);
        br2 = br.cloneBrowser();
        getPage(br2, "/Scripts/pbkdf2.js");
        final String p1 = br2.toString();
        String result1 = null;
        String result2 = null;
        String result3 = null;
        final ScriptEngineManager manager = org.jdownloader.scripting.JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(p1);
            result1 = engine.eval(k2).toString();
            result2 = engine.eval(result1.replace("}(window)", "}")).toString();
            // result3 =
            // engine.eval("$kissenc.decrypt(\"y96f+H5Cxzef4pwA9moW1hl88Qo+JExIfYUtfkxZDbMJ4HbEq/04ZNu+CqY1ISsQ/CE3iuMsyrH+Kopl4tNdcjQbpQCr7e/t5C8wddaHrarndfejJVMURXQ7PzVDu5gl\");").toString();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        System.out.println(1);
    }

}