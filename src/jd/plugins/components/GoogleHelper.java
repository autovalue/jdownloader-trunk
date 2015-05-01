package jd.plugins.components;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;

public class GoogleHelper {

    private static final String META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39 = "<meta\\s+http-equiv=\"refresh\"\\s+content\\s*=\\s*\"(\\d+)\\s*;\\s*url\\s*=\\s*([^\"]+)";
    private Browser             br;
    private boolean             cacheEnabled                                  = false;

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public GoogleHelper(Browser ytbr) {
        this.br = ytbr;

    }

    public void login() {
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n);
                        if (n.isValid()) {
                            return;
                        }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }

        // debug

        accounts = AccountController.getInstance().getAllAccounts("google.com");
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {

                    try {

                        this.login(n);
                        if (n.isValid()) {
                            return;
                        }
                    } catch (final Exception e) {

                        n.setValid(false);
                        return;
                    }

                }
            }
        }
        return;
    }

    private void postPageFollowRedirects(Browser br, String url, LinkedHashMap<String, String> post) throws IOException, InterruptedException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        int wait = 0;
        try {
            br.postPage(url, post);
            url = null;
            if (br.getRedirectLocation() != null) {
                url = br.getRedirectLocation();

            }

            String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
            if (redirect != null) {
                url = Encoding.htmlDecode(redirect[1]);
                wait = Integer.parseInt(redirect[0]) * 1000;
            }
        } finally {
            br.setFollowRedirects(before);
        }
        if (url != null) {
            if (wait > 0) {
                Thread.sleep(wait);
            }
            getPageFollowRedirects(br, url);

        }

    }

    private void getPageFollowRedirects(Browser br, String url) throws IOException, InterruptedException {
        boolean before = br.isFollowingRedirects();
        br.setFollowRedirects(false);
        try {
            int max = 20;
            int wait = 0;
            while (max-- > 0) {
                if (url == null || new URL(url).getHost().toLowerCase(Locale.ENGLISH).contains("youtube.com")) {
                    break;
                }
                if (wait > 0) {
                    Thread.sleep(wait);
                }

                br.getPage(url);
                url = null;
                if (br.getRedirectLocation() != null) {
                    url = br.getRedirectLocation();
                    continue;
                }

                String[] redirect = br.getRegex(META_HTTP_EQUIV_REFRESH_CONTENT_D_S_URL_39_39).getRow(0);
                if (redirect != null) {
                    url = Encoding.htmlDecode(redirect[1]);
                    wait = Integer.parseInt(redirect[0]) * 1000;
                }
            }
        } finally {
            br.setFollowRedirects(before);
        }
    }

    public boolean login(Account account) throws IOException, InterruptedException {

        try {
            this.br.setDebug(true);
            this.br.setCookiesExclusive(true);
            // delete all cookies
            this.br.clearCookies(null);

            br.setCookie("http://google.com", "PREF", "hl=en-GB");
            if (isCacheEnabled() && account.getProperty("cookies") != null) {
                @SuppressWarnings("unchecked")
                HashMap<String, String> cookies = (HashMap<String, String>) account.getProperty("cookies");
                // cookies = null;
                // https://accounts.google.com/CheckCookie?hl=en&checkedDomains=youtube&checkConnection=youtube%3A210%3A1&pstMsg=1&chtml=LoginDoneHtml&service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue&gidl=CAA

                if (cookies != null) {
                    if (cookies.containsKey("SID") && cookies.containsKey("HSID")) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("google.com", key, value);
                        }

                        getPageFollowRedirects(br, "https://accounts.google.com/CheckCookie?hl=en&checkedDomains=youtube&checkConnection=youtube%3A210%3A1&pstMsg=1&chtml=LoginDoneHtml&service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue&gidl=CAA");
                        if (br.containsHTML("accounts/SetSID")) {
                            return true;
                        }
                    }
                }
            }

            this.br.setFollowRedirects(true);
            /* first call to google */

            getPageFollowRedirects(br, "https://accounts.google.com/ServiceLogin?uilel=3&service=youtube&passive=true&continue=http%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26nomobiletemp%3D1%26hl%3Den_US%26next%3D%252Findex&hl=en_US&ltmpl=sso");

            LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();

            post.put("GALX", br.getCookie("http://google.com", "GALX"));
            post.put("continue", "https://www.youtube.com/signin?action_handle_signin=true&app=desktop&feature=sign_in_button&next=%2F&hl=en");
            post.put("service", "youtube");
            post.put("hl", "en");
            post.put("utf8", "☃");
            post.put("pstMsg", "1");
            post.put("dnConn", "");
            post.put("checkConnection", "youtube:210:1");

            post.put("checkedDomains", "youtube");
            post.put("Email", account.getUser());
            post.put("Passwd", account.getPass());
            post.put("signIn", "Sign in");
            post.put("PersistentCookie", "yes");
            post.put("rmShown", "1");

            postPageFollowRedirects(br, "https://accounts.google.com/ServiceLoginAuth", post);
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies cYT = this.br.getCookies("google.com");
            for (final Cookie c : cYT.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("cookies", cookies);
            return br.containsHTML("accounts/SetSID");
        } catch (IOException e) {

            account.setProperty("cookies", null);
            throw e;
        }

    }

    private boolean isCacheEnabled() {
        return cacheEnabled;
    }

}
