package org.telegram.messenger.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class PluginUiOverlay {

    private static final AtomicInteger IDS = new AtomicInteger(1);
    private static final HashMap<Integer, PluginUiPanel> panels = new HashMap<>();
    private static final HashMap<String, ArrayList<Integer>> pluginPanels = new HashMap<>();

    public static int nextId() {
        return IDS.getAndIncrement();
    }

    public static int open(Context context, String pluginId, int id, JSONObject options) {
        LaunchActivity activity = LaunchActivity.instance;
        if (activity == null) {
            return -1;
        }
        try {
            PluginUiPanel panel = new PluginUiPanel(context, pluginId, id);
            panel.applyOptions(options, false);
            activity.getMainContainerFrameLayout().addView(panel);
            panels.put(id, panel);
            synchronized (pluginPanels) {
                ArrayList<Integer> list = pluginPanels.get(pluginId);
                if (list == null) {
                    list = new ArrayList<>();
                    pluginPanels.put(pluginId, list);
                }
                list.add(id);
            }
            return id;
        } catch (Exception e) {
            FileLog.e("PluginUiOverlay: open failed: " + e.getMessage());
            return -1;
        }
    }

    public static void update(int id, JSONObject options) {
        PluginUiPanel panel = panels.get(id);
        if (panel != null) {
            try {
                panel.applyOptions(options, true);
            } catch (Exception e) {
                FileLog.e("PluginUiOverlay: update failed: " + e.getMessage());
            }
        }
    }

    public static void close(int id) {
        PluginUiPanel panel = panels.remove(id);
        if (panel != null) {
            ViewGroup parent = (ViewGroup) panel.getParent();
            if (parent != null) {
                parent.removeView(panel);
            }
            synchronized (pluginPanels) {
                ArrayList<Integer> list = pluginPanels.get(panel.pluginId);
                if (list != null) {
                    list.remove((Integer) id);
                    if (list.isEmpty()) {
                        pluginPanels.remove(panel.pluginId);
                    }
                }
            }
            panel.destroy();
        }
    }

    public static void closeForPlugin(String pluginId) {
        ArrayList<Integer> list;
        synchronized (pluginPanels) {
            list = pluginPanels.get(pluginId);
            if (list == null) {
                return;
            }
            list = new ArrayList<>(list);
        }
        for (Integer id : list) {
            close(id);
        }
    }

    public static void closeAll() {
        Iterator<Integer> it = panels.keySet().iterator();
        while (it.hasNext()) {
            Integer id = it.next();
            it.remove();
            PluginUiPanel panel = panels.get(id);
            if (panel != null) {
                ViewGroup parent = (ViewGroup) panel.getParent();
                if (parent != null) {
                    parent.removeView(panel);
                }
                panel.destroy();
            }
        }
        synchronized (pluginPanels) {
            pluginPanels.clear();
        }
    }

    private static class PluginUiPanel extends FrameLayout {

        final String pluginId;
        final int panelId;

        private final TextView titleView;
        private final TextView closeView;
        private final WebView webView;
        private final FrameLayout titleBar;

        private float startX;
        private float startY;
        private float panelStartX;
        private float panelStartY;
        private boolean dragging;
        private boolean destroyed;

        PluginUiPanel(Context context, String pluginId, int panelId) {
            super(context);
            this.pluginId = pluginId;
            this.panelId = panelId;

            setBackgroundColor(0xFF111116);
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xFF111116);
            background.setCornerRadius(AndroidUtilities.dp(14));
            setBackground(background);
            setElevation(AndroidUtilities.dp(24));

            titleBar = new FrameLayout(context);
            titleBar.setBackgroundColor(0x66222222);
            titleBar.setOnTouchListener(this::onTitleTouch);

            titleView = new TextView(context);
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTextSize(15);
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            titleView.setGravity(Gravity.CENTER_VERTICAL);
            titleView.setText(pluginId);
            titleBar.addView(titleView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.CENTER_VERTICAL));

            closeView = new TextView(context);
            closeView.setText("\u00D7");
            closeView.setTextColor(0xFFFFFFFF);
            closeView.setTextSize(26);
            closeView.setGravity(Gravity.CENTER);
            closeView.setLayoutParams(new FrameLayout.LayoutParams(AndroidUtilities.dp(44), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
            closeView.setOnClickListener(v -> close(panelId));
            titleBar.addView(closeView, closeView.getLayoutParams());

            addView(titleBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(40)));

            webView = buildWebView(context);
            addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP));

            FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams) webView.getLayoutParams();
            contentLp.topMargin = AndroidUtilities.dp(40);
            contentLp.leftMargin = AndroidUtilities.dp(4);
            contentLp.rightMargin = AndroidUtilities.dp(4);
            contentLp.bottomMargin = AndroidUtilities.dp(4);

            setLayoutParams(new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(300),
                    AndroidUtilities.dp(380),
                    Gravity.TOP | Gravity.START
            ));
        }

        @SuppressLint("SetJavaScriptEnabled")
        private WebView buildWebView(Context context) {
            WebView view = new WebView(context);
            view.setBackgroundColor(0xFF111116);
            WebSettings settings = view.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setLoadWithOverviewMode(true);
            view.setWebViewClient(new WebViewClient());
            return view;
        }

        void applyOptions(JSONObject options, boolean update) {
            if (destroyed || options == null) {
                return;
            }
            if (options.has("title")) {
                titleView.setText(options.optString("title", pluginId));
            }
            if (options.has("html")) {
                String html = options.optString("html", "");
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
            if (!options.isNull("width") || !options.isNull("height")) {
                int width = options.has("width") ? AndroidUtilities.dp(options.optInt("width", 300)) : getLayoutParams().width;
                int height = options.has("height") ? AndroidUtilities.dp(options.optInt("height", 380)) : getLayoutParams().height;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
                lp.width = Math.max(AndroidUtilities.dp(160), width);
                lp.height = Math.max(AndroidUtilities.dp(120), height);
                setLayoutParams(lp);
            }
            if (options.has("x") || options.has("y")) {
                setX(AndroidUtilities.dp(options.optInt("x", 8)));
                setY(AndroidUtilities.dp(options.optInt("y", 8)));
            }
            if (options.has("visible")) {
                setVisibility(options.optBoolean("visible", true) ? View.VISIBLE : View.GONE);
            }
        }

        private boolean onTitleTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    dragging = true;
                    startX = event.getRawX();
                    startY = event.getRawY();
                    panelStartX = getX();
                    panelStartY = getY();
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (dragging) {
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        float nx = panelStartX + dx;
                        float ny = panelStartY + dy;
                        ViewGroup parent = (ViewGroup) getParent();
                        if (parent != null) {
                            nx = Math.max(0, Math.min(nx, parent.getWidth() - getWidth()));
                            ny = Math.max(0, Math.min(ny, parent.getHeight() - getHeight()));
                        }
                        setX(nx);
                        setY(ny);
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    dragging = false;
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
            }
            return false;
        }

        void destroy() {
            destroyed = true;
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }
    }
}