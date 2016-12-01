//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDHexUtils;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "https?://(?:www\\.)?((wtrns\\.fr|we\\.tl)/[\\w\\-]+|wetransfer\\.com/downloads/[a-z0-9]+/[a-z0-9]+(/[a-z0-9]+)?)" })
public class WeTransferCom extends PluginForHost {

    private String hash   = null;
    private String code   = null;
    private String dllink = null;
    private String param  = null;

    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wetransfer.info/terms/";
    }

    private String getAMFRequest() {
        final String data = "0A000000020200" + getHexLength(code) + JDHexUtils.getHexString(code) + "0200" + getHexLength(hash) + JDHexUtils.getHexString(hash);
        return JDHexUtils.toString("000000000001002177657472616E736665722E446F776E6C6F61642E636865636B446F776E6C6F616400022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        final String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        String dlink = link.getDownloadURL();
        if (dlink.matches("https?://(wtrns\\.fr|we\\.tl)/[\\w\\-]+")) {
            br.setFollowRedirects(false);
            br.getPage(dlink);
            dlink = br.getRedirectLocation();
            if (dlink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dlink.contains("/error")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        hash = new Regex(dlink, "([0-9a-f]+)$").getMatch(0);
        code = new Regex(dlink, "wetransfer\\.com/downloads/([a-z0-9]+)/").getMatch(0);
        if (hash == null || code == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Allow redirects for change to https
        br.setFollowRedirects(true);
        br.getPage(dlink);
        String recepientID = br.getRegex("data-recipient=\"([a-z0-9]+)\"").getMatch(0);
        if (recepientID == null) {
            recepientID = "";
        }
        final String json = br.getRegex(">\\s*var _preloaded_transfer_\\s*=\\s*(\\{.*?\\});\\s*</script>").getMatch(0);
        final Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);

        String filename1 = (String) JavaScriptEngineFactory.walkJson(map, "files/{0}/name");
        final long filesize1 = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(map, "files/{0}/size"), 0);
        final String mainpage = new Regex(dlink, "(https?://(www\\.)?([a-z0-9\\-\\.]+\\.)?wetransfer\\.com/)").getMatch(0);
        br.getPage(mainpage + "api/v1/transfers/" + code + "/download?recipient_id=" + recepientID + "&security_hash=" + hash + "&password=&ie=false&ts=" + System.currentTimeMillis());
        if ("invalid_transfer".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = PluginJSonUtils.getJsonValue(br, "direct_link");
        if (dllink == null) {
            // 20160415-raztoki
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            final LinkedHashMap<String, Object> field = (LinkedHashMap<String, Object>) entries.get("fields");
            final String action = (String) JavaScriptEngineFactory.walkJson(entries, "formdata/action");
            final String method = (String) JavaScriptEngineFactory.walkJson(entries, "formdata/method");
            if (action == null || field == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if ("GET".equalsIgnoreCase(method)) {
                dllink = action + "?" + processJson(field.toString().substring(1, field.toString().length() - 1));
            } else {
                dllink = action;
                param = processJson(field.toString().substring(1, field.toString().length() - 1));
            }
        }
        if (dllink != null) {
            String filename = new Regex(Encoding.htmlDecode(dllink), "filename=([^&]+)").getMatch(0);
            if (filename == null) {
                filename = PluginJSonUtils.getJsonValue(br, "filename");
            }
            if (filename == null) {
                filename = filename1;
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (filesize1 > 0) {
                link.setDownloadSize(filesize1);
            }
        } else {
            /** Old way */
            // AMF-Request
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Accept-Charset", null);
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Content-Type", "application/x-amf");
            br.getHeaders().put("Referer", "https://www.wetransfer.com/index.swf?nocache=" + String.valueOf(System.currentTimeMillis() / 1000));
            br.postPageRaw("https://v1.wetransfer.com/amfphp/gateway.php", getAMFRequest());

            /* TODO: remove me after 0.9xx public */
            br.getHeaders().put("Content-Type", null);

            // successfully request?
            final int rC = br.getHttpConnection().getResponseCode();
            if (rC != 200) {
                logger.warning("File not found! Link: " + dlink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            final StringBuffer sb = new StringBuffer();
            /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
            for (final byte element : br.toString().getBytes()) {
                if (element < 127) {
                    if (element > 31) {
                        sb.append((char) element);
                    } else {
                        sb.append("#");
                    }
                }
            }
            final String result = sb.toString();
            if (new Regex(result, "(download_error_no_download|download_error_file_expired)").matches()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            final String filename = new Regex(result, "#filename[#]+\\$?([^<>#]+)").getMatch(0);
            if (filesize1 == 0) {
                final String filesize = new Regex(result, "#size[#]+(\\d+)[#]+").getMatch(0);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            } else {
                link.setDownloadSize(filesize1);
            }
            dllink = new Regex(result, "#awslink[#]+\\??([^<>#]+)").getMatch(0);

            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // More chunks are possible for some links but not for all
        if (param != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, param, true, -2);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error while downloading your file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private String processJson(final String s) {
        String fields = s;
        final String callback = new Regex(fields, "callback=(\\{.*?\\}\\}$)").getMatch(0);
        fields = fields.replace(callback, "-JDTEMPREMOVED-");
        String postString = fields.replace(", ", "&");
        postString = postString.replace("-JDTEMPREMOVED-", Encoding.urlEncode(callback));
        return postString;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}