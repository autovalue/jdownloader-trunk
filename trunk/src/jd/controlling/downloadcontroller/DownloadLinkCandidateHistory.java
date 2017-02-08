package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;

public class DownloadLinkCandidateHistory {

    public static interface DownloadLinkCandidateHistorySelector {
        boolean select(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result);
    }

    private final LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> history = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();

    protected DownloadLinkCandidateHistory() {
    }

    public synchronized boolean attach(DownloadLinkCandidate candidate) {
        if (history.containsKey(candidate)) {
            return false;
        }
        try {
            DownloadLink link = candidate.getLink();
            if (link != null) {
                link.addHistoryEntry(HistoryEntry.create(candidate));

            }
        } catch (Throwable e) {
            // just to be sure
            e.printStackTrace();
        }
        history.put(candidate, null);
        return true;
    }

    public synchronized boolean dettach(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (history.containsKey(candidate) && history.get(candidate) == null) {
            try {
                DownloadLink link = candidate.getLink();
                if (link != null) {
                    HistoryEntry history = link.getLatestHistoryEntry();
                    if (history != null && history.getCandidate() == candidate) {
                        HistoryEntry.updateResult(history, candidate, result);
                    } else {
                        System.out.println("Candidate Misnach");
                    }

                } else {
                    System.out.println("No LInk");
                }
            } catch (Throwable e) {
                // just to be sure
                e.printStackTrace();
            }
            history.put(candidate, result);
            return true;
        }
        return false;
    }

    protected Map<DownloadLinkCandidate, DownloadLinkCandidateResult> getHistory() {
        return history;
    }

    @Deprecated
    /**
     * @deprecated Use     public List<DownloadLinkCandidateResult> getResults(final CachedAccount cachedAccount) {
     * @param candidate
     * @return
     */
    public List<DownloadLinkCandidateResult> getResults(final DownloadLinkCandidate candidate) {
        return getResults(candidate.getCachedAccount());
    }

    public List<DownloadLinkCandidateResult> getResults(final CachedAccount cachedAccount) {
        return selectResults(new DownloadLinkCandidateHistorySelector() {

            @Override
            public boolean select(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
                return candidate != null && cachedAccount.equals(candidate.getCachedAccount());
            };
        });
    }

    public List<DownloadLinkCandidateResult> selectResults(DownloadLinkCandidateHistorySelector selector) {
        return selectResults(this, selector);
    }

    public static List<DownloadLinkCandidateResult> selectResults(Collection<DownloadLinkCandidateHistory> histories, DownloadLinkCandidateHistorySelector selector) {
        final List<DownloadLinkCandidateResult> ret = new ArrayList<DownloadLinkCandidateResult>();
        for (DownloadLinkCandidateHistory history : histories) {
            ret.addAll(selectResults(history, selector));
        }
        return ret;
    }

    public static List<DownloadLinkCandidateResult> selectResults(DownloadLinkCandidateHistory history, DownloadLinkCandidateHistorySelector selector) {
        final List<DownloadLinkCandidateResult> ret = new ArrayList<DownloadLinkCandidateResult>();
        synchronized (history) {
            final Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it = history.getHistory().entrySet().iterator();
            while (it.hasNext()) {
                final Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next = it.next();
                final DownloadLinkCandidate candidate = next.getKey();
                final DownloadLinkCandidateResult result = next.getValue();
                if (candidate != null && result != null) {
                    if (selector == null || selector.select(candidate, result)) {
                        ret.add(result);
                    }
                }
            }
        }
        return ret;
    }

    public int size() {
        return history.size();
    }

    public DownloadLinkCandidateResult getBlockingHistory(DownloadLinkCandidateSelector selector, DownloadLinkCandidate candidate) {
        if (selector != null && candidate != null) {
            final List<DownloadLinkCandidateResult> results = getResults(candidate.getCachedAccount());
            final Iterator<DownloadLinkCandidateResult> it = results.iterator();
            while (it.hasNext()) {
                final DownloadLinkCandidateResult next = it.next();
                switch (next.getResult()) {
                case IP_BLOCKED:
                case HOSTER_UNAVAILABLE:
                    /* evaluated in onDetach and handled by proxyInfoHistory */
                    break;
                case CONDITIONAL_SKIPPED:
                case ACCOUNT_INVALID:
                case ACCOUNT_ERROR:
                case ACCOUNT_UNAVAILABLE:
                    /* already handled in onDetach */
                    break;
                case FINISHED:
                case FINISHED_EXISTS:
                case SKIPPED:
                case STOPPED:
                case FAILED:
                case FAILED_EXISTS:
                case OFFLINE_TRUSTED:
                    /* these results(above) should have ended in removal of DownloadLinkHistory */
                case PROXY_UNAVAILABLE:
                case CONNECTION_TEMP_UNAVAILABLE:
                    /* if we end up here(results above) -> find the bug :) */
                    throw new WTFException("This should not happen! " + next.getResult() + " should already be handled in onDetach!");
                case FILE_UNAVAILABLE:
                case CONNECTION_ISSUES:
                    /* these results(above) can TEMP. block */
                    if (next.getRemainingTime() <= 0) {
                        break;
                    }
                case PLUGIN_DEFECT:
                case ACCOUNT_REQUIRED:
                case FATAL_ERROR:
                    return next;
                case CAPTCHA:
                case FAILED_INCOMPLETE:
                case RETRY:
                    /* these results(above) do NEVER block */
                    break;
                }
            }
            final DownloadLinkCandidateResult block = selector.getBlockingDownloadLinkCandidateResult(candidate, results, this);
            if (block != null) {
                return block;
            }
        }
        return null;
    }
}
