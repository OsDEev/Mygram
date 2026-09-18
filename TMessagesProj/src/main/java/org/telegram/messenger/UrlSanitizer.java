package org.telegram.messenger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlSanitizer {

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(https?://[^\\s<>\"']+)");

    private static final String[] TRACKER_PARAMS = {
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
            "fbclid", "gclid", "gclsrc", "dclid", "mc_cid", "mc_eid", "igshid",
            "igsh", "ymclid", "msclkid", "twclid", "li_fat_id", "_hsenc", "_hsmi",
            "ref_src", "ref_url"
    };

    private static String sanitizeUrl(String url) {
        String cleaned = url;
        int qIndex = cleaned.indexOf('?');
        if (qIndex >= 0) {
            String base = cleaned.substring(0, qIndex);
            String query = cleaned.substring(qIndex + 1);
            StringBuilder sb = new StringBuilder();
            String[] params = query.split("&");
            boolean first = true;
            for (String param : params) {
                if (param.isEmpty()) continue;
                String key;
                int eq = param.indexOf('=');
                key = eq >= 0 ? param.substring(0, eq) : param;
                boolean isTracker = false;
                for (String tracker : TRACKER_PARAMS) {
                    if (key.equalsIgnoreCase(tracker)) {
                        isTracker = true;
                        break;
                    }
                }
                if (isTracker) continue;
                if (!first) sb.append('&');
                sb.append(param);
                first = false;
            }
            cleaned = first ? base : base + "?" + sb.toString();
        }
        String host;
        int slash = cleaned.indexOf('/', cleaned.indexOf("://") + 3);
        if (slash < 0) slash = cleaned.indexOf('?');
        if (slash < 0) {
            host = cleaned;
        } else {
            host = cleaned.substring(0, slash);
        }
        String hostLower = host.toLowerCase();
        String fixedHost = null;
        if (hostLower.startsWith("https://x.com") || hostLower.startsWith("https://www.x.com")
                || hostLower.startsWith("https://twitter.com") || hostLower.startsWith("https://www.twitter.com")) {
            fixedHost = "https://vxtwitter.com" + (host.length() >= hostLower.length() ? "" : "");
        }
        if (fixedHost != null) {
            String pathAndQuery = cleaned.substring(host.length());
            cleaned = fixedHost + pathAndQuery;
        }
        return cleaned;
    }

    public static String sanitizeText(String text) {
        if (text == null) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group();
            String cleaned = sanitizeUrl(url);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(cleaned));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}