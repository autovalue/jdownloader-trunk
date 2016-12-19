package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import jd.PluginWrapper;
import jd.http.requests.PostRequest;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UnavailableHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision: 35512 $", interfaceVersion = 3, names = { "linkifier.com" }, urls = { "" })
public class LinkifierCom extends PluginForHost {

    private static final String API_KEY = "d046c4309bb7cabd19f49118a2ab25e0";

    public LinkifierCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://www.linkifier.com");
    }

    @Override
    public String getAGBLink() {
        return "https://www.linkifier.com/terms-of-use/";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final String username = account.getUser();
        if (username == null || !username.matches("^.+?@.+?\\.[^\\.]+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use your email address to login", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final AccountInfo ai = new AccountInfo();
        final HashMap<String, Object> userJson = new HashMap<String, Object>();
        userJson.put("login", username);
        userJson.put("md5Pass", Hash.getMD5(account.getPass()));
        userJson.put("apiKey", API_KEY);
        final PostRequest userRequest = new PostRequest("https://api.linkifier.com/downloadapi.svc/user");
        userRequest.setContentType("application/json; charset=utf-8");
        userRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(userJson));
        final HashMap<String, Object> userResponse = JSonStorage.restoreFromString(br.getPage(userRequest), TypeRef.HASHMAP);
        if (Boolean.TRUE.equals(userResponse.get("isActive")) && !Boolean.TRUE.equals(userResponse.get("hasErrors"))) {
            if ("unlimited".equalsIgnoreCase(String.valueOf(userResponse.get("extraTraffic")))) {
                ai.setUnlimitedTraffic();
            }
            final Number expiryDate = (Number) userResponse.get("expirydate");
            if (expiryDate != null) {
                ai.setValidUntil(expiryDate.longValue());
                if (!ai.isExpired()) {
                    final PostRequest hosterRequest = new PostRequest("https://api.linkifier.com/DownloadAPI.svc/hosters");
                    hosterRequest.setContentType("application/json; charset=utf-8");
                    hosterRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(userJson));
                    final HashMap<String, Object> hosterResponse = JSonStorage.restoreFromString(br.getPage(hosterRequest), TypeRef.HASHMAP);
                    final List<Map<String, Object>> hosters = (List<Map<String, Object>>) hosterResponse.get("hosters");
                    if (hosters != null) {
                        final List<String> supportedHosts = new ArrayList<String>();
                        for (Map<String, Object> host : hosters) {
                            final String hostername = host.get("hostername") != null ? String.valueOf(host.get("hostername")) : null;
                            if (Boolean.TRUE.equals(host.get("isActive")) && StringUtils.isNotEmpty(hostername)) {
                                supportedHosts.add(hostername);
                            }
                        }
                        ai.setMultiHostSupport(this, supportedHosts);
                    }
                    return ai;
                }
            }
        }
        final String errorMsg = userResponse.get("ErrorMSG") != null ? String.valueOf(userResponse.get("ErrorMSG")) : null;
        if (StringUtils.containsIgnoreCase(errorMsg, "Error verifying api key")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private static WeakHashMap<Account, HashMap<String, UnavailableHost>> hostUnavailableMap = new WeakHashMap<Account, HashMap<String, UnavailableHost>>();

    private void tempUnavailableHoster(final DownloadLink downloadLink, final Account account, final long timeout, final String reason) throws PluginException {
        final UnavailableHost nue = new UnavailableHost(System.currentTimeMillis() + timeout, reason);
        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, UnavailableHost>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            unavailableMap.put(downloadLink.getHost(), nue);
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, UnavailableHost> unavailableMap = hostUnavailableMap.get(null);
            UnavailableHost nue = unavailableMap != null ? unavailableMap.get(downloadLink.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this multihoster: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(null);
                    }
                }
            }
            unavailableMap = hostUnavailableMap.get(account);
            nue = unavailableMap != null ? unavailableMap.get(downloadLink.getHost()) : null;
            if (nue != null) {
                final Long lastUnavailable = nue.getErrorTimeout();
                final String errorReason = nue.getErrorReason();
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable for this account: " + errorReason != null ? errorReason : "via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        final HashMap<String, Object> downloadJson = new HashMap<String, Object>();
        downloadJson.put("login", account.getUser());
        downloadJson.put("md5Pass", Hash.getMD5(account.getPass()));
        downloadJson.put("apiKey", API_KEY);
        downloadJson.put("url", downloadLink.getDefaultPlugin().buildExternalDownloadURL(downloadLink, this));
        final PostRequest downloadRequest = new PostRequest("https://api.linkifier.com/downloadapi.svc/download");
        downloadRequest.setContentType("application/json; charset=utf-8");
        downloadRequest.setPostBytes(JSonStorage.serializeToJsonByteArray(downloadJson));
        final HashMap<String, Object> downloadResponse = JSonStorage.restoreFromString(br.getPage(downloadRequest), TypeRef.HASHMAP);
        if (Boolean.FALSE.equals(downloadResponse.get("hasErrors"))) {
            final String url = downloadResponse.get("url") != null ? String.valueOf(downloadResponse.get("url")) : null;
            if (StringUtils.isEmpty(url)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setConnectTimeout(120 * 1000);
            br.setReadTimeout(120 * 1000);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "json") || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dl.getConnection().isContentDisposition()) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
            return;
        }
        final String errorMsg = downloadResponse.get("ErrorMSG") != null ? String.valueOf(downloadResponse.get("ErrorMSG")) : null;
        if (errorMsg != null) {
            if (StringUtils.containsIgnoreCase(errorMsg, "Error verifying api key")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Could not find a customer with those credentials")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Account expired")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Customer reached daily limit for current hoster")) {
                tempUnavailableHoster(downloadLink, account, 60 * 60 * 1000l, errorMsg);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Accounts are maxed out for current hoster")) {
                tempUnavailableHoster(downloadLink, account, 60 * 60 * 1000l, errorMsg);
            }
            if (StringUtils.containsIgnoreCase(errorMsg, "Downloads blocked until")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, errorMsg);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

}
