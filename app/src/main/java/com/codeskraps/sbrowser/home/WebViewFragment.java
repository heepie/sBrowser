package com.codeskraps.sbrowser.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.codeskraps.sbrowser.R;
import com.codeskraps.sbrowser.misc.L;
import com.codeskraps.sbrowser.misc.SBrowserData;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewFragment extends Fragment {
    private static final String TAG = WebViewFragment.class.getSimpleName();

    private ProgressBar prgBar = null;
    private WebView webView = null;
    private String defaultUserAgent = null;
    private String title;
    private String video;
    private boolean launched = false;
    private AlertDialog.Builder alert;

    private boolean webLoading;

    public static WebViewFragment getInstance() {
        WebViewFragment fragment = new WebViewFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        L.v(TAG, "onCreateView");
        return inflater.inflate(R.layout.webview, container, false);
    }

    @SuppressLint("NewApi")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        L.v(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        webView = (WebView) view.findViewById(R.id.webview);
        prgBar = (ProgressBar) view.findViewById(R.id.prgBar);

        ((SBrowserActivity) getActivity()).setWebView(this, webView);
        webLoading = false;
        registerForContextMenu(webView);

        defaultUserAgent = webView.getSettings().getUserAgentString();

        WebSettings ws = webView.getSettings();
        ws.setLoadsImagesAutomatically(true);
        ws.setUseWideViewPort(true);
        ws.setAllowFileAccess(true);
        ws.setLoadWithOverviewMode(true);
        ws.setDomStorageEnabled(true);
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        // ws.setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            ws.setDisplayZoomControls(true);

        webView.setInitialScale(200);
        webView.setNetworkAvailable(true);
        webView.setWebViewClient(new WebViewActivityClient());
        webView.setWebChromeClient(new WebChromeActivityClient());
        webView.setDownloadListener(new DownloadActivityListener());

        SBrowserData sBrowserData = ((SBrowserApplication) getActivity().getApplication())
                .getsBrowserData();
        L.i(TAG, "onCreate loadUrl:" + sBrowserData.getetxtHome());
        webView.loadUrl(sBrowserData.getetxtHome());
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onResume() {
        L.v(TAG, "onResume");
        super.onResume();

        SBrowserData sBrowserData = ((SBrowserApplication) getActivity().getApplication())
                .getsBrowserData();

        L.v(TAG, "isJavascript enabled:" + sBrowserData.isChkJavascript());
        L.v(TAG, "PlugingState:" + sBrowserData.getLstflash());
        webView.getSettings().setJavaScriptEnabled(sBrowserData.isChkJavascript());

        // @formatter:off
		switch (sBrowserData.getLstflash()) {
		case 0: webView.getSettings().setPluginState(PluginState.ON); break;
		case 1: webView.getSettings().setPluginState(PluginState.ON_DEMAND); break;
		case 2: webView.getSettings().setPluginState(PluginState.OFF); break;
		}
		// @formatter:on

        String userAgent = webView.getSettings().getUserAgentString();
        String[] lstUserAgentArray = getResources().getStringArray(
                R.array.prefs_user_agent_human_value);

        switch (sBrowserData.getUserAgent()) {
            case 0:
                userAgent = defaultUserAgent;
                break;
            case 1:
                userAgent = userAgent.replaceAll("Android", lstUserAgentArray[1]);
                userAgent = userAgent.replaceAll("Chrome", lstUserAgentArray[1]);
                userAgent = userAgent.replaceAll("Ipad", lstUserAgentArray[1]);
                userAgent = userAgent.replaceAll("Mobile", "Desktop");
                break;
            case 2:
                userAgent = userAgent.replaceAll("Android", lstUserAgentArray[2]);
                userAgent = userAgent.replaceAll("Firefox", lstUserAgentArray[2]);
                userAgent = userAgent.replaceAll("Ipad", lstUserAgentArray[2]);
                userAgent = userAgent.replaceAll("Mobile", "Desktop");
                break;
            case 3:
                userAgent = userAgent.replaceAll("Android", lstUserAgentArray[3]);
                userAgent = userAgent.replaceAll("Chrome", lstUserAgentArray[3]);
                userAgent = userAgent.replaceAll("Firefox", lstUserAgentArray[3]);
                userAgent = userAgent.replaceAll("Mobile", "Desktop");
                break;
        }

        webView.getSettings().setUserAgentString(userAgent);
        L.d(TAG, "User Agent: " + webView.getSettings().getUserAgentString());

        if (sBrowserData.isSelected()) {
            L.i(TAG, "onResume loadUrl:" + sBrowserData.getSaveState());
            webView.loadUrl(sBrowserData.getSaveState());
            sBrowserData.setSelected(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SBrowserData sBrowserData = ((SBrowserApplication) getActivity().getApplication())
                .getsBrowserData();
        sBrowserData.setSelected(true);
        sBrowserData.setSaveState(webView.getUrl());
    }

    private class WebViewActivityClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            L.d(TAG, "shouldOverrideUrlLoading: " + url);
            L.d(TAG, "MimeType: " + MimeTypeMap.getFileExtensionFromUrl(url));

            if (("mp4".equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))
                    || ("3gp"
                    .equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))) {

                Intent intent = new Intent();
                intent.setClass(getActivity(), VideoPlayer.class);
                intent.setData(Uri.parse(url));
                intent.putExtra("type", 0);
                startActivity(intent);

            } else if (("ppt"
                    .equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))
                    || ("doc"
                    .equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))
                    || ("pdf"
                    .equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))
                    || ("apk"
                    .equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(url)))) {

                DownloadManager dm = (DownloadManager) getActivity().getSystemService(
                        Context.DOWNLOAD_SERVICE);
                Request request = new Request(Uri.parse(url));
                dm.enqueue(request);
                Intent i = new Intent();
                i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                startActivity(i);

            } else if (url.startsWith("market://")) {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                goToMarket.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(goToMarket);

            } else {
                if (!url.equals("about:blank") && !url.equals(view.getUrl())) {
                    L.v(TAG, "Loading url:" + url);
                    view.loadUrl(url);
                    new HandleVideo().execute(url);
                }
            }
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
            L.d(TAG, "onReceivedSslError");
        }

        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            L.d(TAG, "Error: " + description + ", " + failingUrl);
            // Toast.makeText(SBrowserActivity.this,
            // "sBrowser - Something when wrong!!!", Toast.LENGTH_SHORT).show();
        }
    }

    private class HandleVideo extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Document doc = Jsoup.connect(params[0]).get();
                Elements metalinks = doc.select("meta[property=og:type]");
                if (!metalinks.isEmpty()) {
                    String content = metalinks.first().attr("content");
                    if ("video.movie".equals(content)) {
                        metalinks = doc.select("meta[property=og:title]");
                        if (!metalinks.isEmpty()) {
                            title = metalinks.first().attr("content");
                        }

                        metalinks = doc.select("div[id=player]");
                        if (!metalinks.isEmpty()) {
                            Elements elements = metalinks.select("script");
                            if (!elements.isEmpty()) {
                                Iterator<Element> iter = elements.iterator();
                                while (iter.hasNext()) {
                                    String html = iter.next().html();
                                    L.w(TAG, html);
                                    if (html.contains("HTML5Player")) {
                                        html = html.substring(html.indexOf("HTML5Player"));
                                        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(html);
                                        while (m.find()) {
                                            L.v(TAG, "m:" + m.group(1));
                                            String[] attr = m.group(1).split(",");
                                            L.v(TAG, "attr:" + attr[3]);
                                            int index1 = attr[3].indexOf("\'") + 1;
                                            int index2 = attr[3].indexOf("\'", index1);
                                            String newVideo = attr[3].substring(index1, index2);
                                            L.v(TAG, "video:" + newVideo);
                                            if (!newVideo.equals(video)) {
                                                launched = false;
                                                video = newVideo;
                                            }
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        return false;
                    }
                }

            } catch (Exception e) {
                L.e(TAG, "Handled - HandleVideo:" + e, e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (getActivity() == null) return;
            if (aBoolean && !launched && alert == null) {
                alert = new AlertDialog.Builder(getActivity());
                alert.setCancelable(false);
                alert.setTitle(getResources().getString(R.string.alertVideoFound_title));
                alert.setMessage(getResources().getString(R.string.alertVideoFound_summary, title));
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), VideoPlayer.class);
                        intent.setData(Uri.parse(video));
                        intent.putExtra("type", 0);
                        startActivity(intent);
                        launched = true;
                        dialog.dismiss();
                        alert = null;
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        alert = null;
                    }
                });
                alert.show();
            }
        }
    }

    private class WebChromeActivityClient extends WebChromeClient {
        public void onProgressChanged(WebView view, int progress) {

            Activity act = getActivity();

            if (!webLoading) {
                if (act != null) ((SBrowserActivity) act).setStopButton();
                webLoading = true;
                prgBar.setVisibility(View.VISIBLE);
            }
            prgBar.setProgress(progress);

            if (progress == 100) {
                prgBar.setVisibility(View.GONE);
                if (act != null) ((SBrowserActivity) act).setBackForwardButtons();
                webLoading = false;
            }
        }
    }

    private class DownloadActivityListener implements DownloadListener {
        public void onDownloadStart(final String url, String userAgent, String contentDisposition,
                                    String mimetype, long contentLength) {
            L.d(TAG, "onDownloadStart");
        }
    }

    public boolean isReloading() {
        return webLoading;
    }

    public void reload() {
        launched = false;
        webView.reload();
    }
}