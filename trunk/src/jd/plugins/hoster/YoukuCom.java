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
package jd.plugins.hoster;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "youku.com" }, urls = { "https?://k\\.youku\\.com/player/getFlvPath/.*?fileid/[A-F0-9\\-]+.+|https?://[A-Za-z0-9\\-]+\\.youku\\.com/playlist/m3u8.*?psid=[a-f0-9]{32}.+" })
public class YoukuCom extends antiDDoSForHost {
    public YoukuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://v.youku.com/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    public static Browser prepBR(final Browser br, final String host) {
        br.setFollowRedirects(true);
        return br;
    }

    /**
     * TODO: Add refreshDownloadlink function, add errorhandling (GEO-blocked content!!!), check whether tudou.com belongs to youku.com and
     * integrate it as well.
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        dllink = link.getDownloadURL();
        if (dllink.contains("m3u8")) {
            checkFFProbe(link, "Download a HLS Stream");
            final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                server_issues = true;
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    link.setDownloadSize(estimatedSize);
                }
            }
        } else {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    if (link.getFinalFileName() == null) {
                        /* Only use server-filename e.g. for URLs sent over via Flashgot or similar. */
                        link.setFinalFileName(getFileNameFromHeader(con));
                    }
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    this.server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handleGeneralErrors();
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            /*
             * If trailer download is possible but dllink == null in theory this would be a PLUGIN_DEFECT but I think that premiumonly
             * message is more suitable here as a trailer is usually not what you'd want to download.
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (dllink.contains("m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setProperty("free_directlink", dllink);
            dl.startDownload();
        }
    }

    private void handleGeneralErrors() throws PluginException {
        /* Fill me up */
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        // return account != null;
        return true;
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        /* Only original plugin is always allowed to download. */
        final boolean is_this_plugin = downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        if (is_this_plugin) {
            return true;
        } else {
            return false;
        }
    }

    public static String getURLName(final String inputurl) {
        final String url_name;
        if (inputurl.matches(".+v_show/id_.+")) {
            url_name = new Regex(inputurl, "/v_show/id_(.+)\\.html$").getMatch(0);
        } else if (inputurl.contains("m3u8")) {
            /* Ummm not sure if that is a good idea but this function has to return something ... */
            url_name = new Regex(inputurl, "psid=([a-f0-9]{32})").getMatch(0);
        } else {
            url_name = new Regex(inputurl, "/fileid/([A-F0-9\\-]+)").getMatch(0);
        }
        return url_name;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the youku.com plugin.";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return YoukuComConfigInterface.class;
    }

    public static interface YoukuComConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }

            public String getAddUnknownQualitiesEnabled_label() {
                return _JDT.T.lit_add_unknown_formats();
            }

            public String getGrabHTTPMp4_1080pEnabled_label() {
                return "Grab 1080p HTTP (mp4)?";
            }

            public String getGrabHTTPMp4_720pEnabled_label() {
                return "Grab 720p HTTP (mp4)?";
            }

            public String getGrabHTTPMp4_540pEnabled_label() {
                return "Grab 540p HTTP (mp4)?";
            }

            public String getGrabHTTPMp4_360pEnabled_label() {
                return "Grab 360p HTTP (mp4)?";
            }

            public String getGrabHLSMp4_1080pEnabled_label() {
                return "Grab 1080p HLS (mp4)?";
            }

            public String getGrabHLSMp4_720pEnabled_label() {
                return "Grab 720p HLS (mp4)?";
            }

            public String getGrabHLSMp4_540pEnabled_label() {
                return "Grab 540p HLS (mp4)?";
            }

            public String getGrabHLSMp4_360pEnabled_label() {
                return "Grab 360p HLS (mp4)?";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(22)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4_1080pEnabled();

        void setGrabHTTPMp4_1080pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTPMp4_720pEnabled();

        void setGrabHTTPMp4_720pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTPMp4_540pEnabled();

        void setGrabHTTPMp4_540pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(120)
        boolean isGrabHTTPMp4_360pEnabled();

        void setGrabHTTPMp4_360pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(130)
        boolean isGrabHLSMp4_1080pEnabled();

        void setGrabHLSMp4_1080pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(140)
        boolean isGrabHLSMp4_720pEnabled();

        void setGrabHLSMp4_720pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(150)
        boolean isGrabHLSMp4_540pEnabled();

        void setGrabHLSMp4_540pEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(160)
        boolean isGrabHLSMp4_360pEnabled();

        void setGrabHLSMp4_360pEnabled(boolean b);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}