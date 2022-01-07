package com.falcofemoralis.hdrezkaapp.clients

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.objects.Film


class PlayerWebViewClient(val context: Context, val mainView: IConnection, val film: Film, val callback: () -> Unit) : WebViewClient() {

    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!checkUrl(request.url.toString())) {
            view.loadUrl(request.url.toString())
        }
        return true
    }

    // Для старых устройств
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (!checkUrl(url)) {
            view.loadUrl(url)
        }
        return true
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        /*      if (error?.errorCode == ERROR_TIMEOUT) {
                  mainView.showConnectionError(IConnection.ErrorType.TIMEOUT, error.toString())
              }*/
        super.onReceivedError(view, request, error)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        // block advert
        val script = "XMLHttpRequest.prototype.open = (function (open) {" +
                "    return function (method, url, async) {" +
                "        if (url.match(/franecki.net/g) || url.match(/biocdn.net/g) || url.match(/franeski.net/g) || url.match(/reichelcormier.bid/g) || url.match(/track.adpod.in.bid/g)) {" +
                "            console.log('blocked');" +
                "        } else {" +
                "            open.apply(this, arguments);" +
                "        }" +
                "    };" +
                "})(XMLHttpRequest.prototype.open);"
        view?.evaluateJavascript(script, null)

        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        // Move player at top
        val script1 = "javascript:document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=device-width,initial-scale=1.0');" +
                "var idsToHide = ['top-head', 'top-nav', 'comments-form', 'hd-comments-list', 'hd-comments-navigation', 'footer'];" +
                "for (var i = 0; i < idsToHide.length; ++i) {" +
                "    var el = document.getElementById(idsToHide[i]);" +
                "    if (el) {" +
                "        el.style.setProperty('display', 'none', 'important');" +
                "    }" +
                "}" +
                "var classesToHide = ['post__title'," +
                "    'b-post__origtitle'," +
                "    'b-post__infotable clearfix'," +
                "    'b-post__description'," +
                "    'b-post__mixedtext'," +
                "    'b-post__lastepisodeout'," +
                "    'b-post__social_holder_wrapper'," +
                "    'b-post__rating_table'," +
                "    'b-post__actions'," +
                "    'b-sidetitle'," +
                "    'b-sidelist__holder'," +
                "    'b-post__schedule'," +
                "    'b-post__qa_list_block'," +
                "    'b-post__mtitle'," +
                "    'b-wrapper nopadd'," +
                "    'b-post__status_wrapper'," +
                "    'b-post__support_holder'," +
                "    'b-post__title'," +
                "    'b-sidetitle'," +
                "    'b-post__partcontent'," +
                "    'b-post__infolast'];" +
                "for (var i = 0; i < classesToHide.length; ++i) {" +
                "    var els = document.getElementsByClassName(classesToHide[i]);" +
                "    for (var j = 0; j < els.length; ++j) {" +
                "        if (els[j]) {" +
                "            els[j].style.setProperty('display', 'none', 'important');" +
                "        }" +
                "    }" +
                "}"
        view?.evaluateJavascript(script1, null)

        val script2 = "javascript: (function() {" +
                "document.body.style.minWidth = 'unset';" +
                "document.getElementsByTagName('html')[0].style.height = 'unset';" +
                "" + // fix width translations
                "var translatorArray = document.getElementsByClassName('b-translator__item');" +
                "for (var i = 0; i < translatorArray.length; i++) {" +
                "    translatorArray[i].style.setProperty('min-width', '100%', 'important');" +
                "    translatorArray[i].style.setProperty('width', 'unset', 'important');" +
                "}" +
                "var translatorBlock = document.getElementsByClassName(\"b-translators__block\");" +
                "if (translatorBlock.length > 0) {" +
                "    translatorBlock[0].style.setProperty('padding-right', '10px', 'important');" +
                "}" +
                "" +
                "var translatorList = document.getElementsByClassName(\"b-translators__list\");" +
                "if (translatorList.length > 0) {" +
                "    translatorList[0].style.setProperty('padding-right', 'unset', 'important');" +
                "}" +
                "var playercont = document.getElementById('cdnplayer-container');" +
                "if (playercont == null) {" +
                "    playercont = document.getElementById('ownplayer');" +
                "}" +
                "if (playercont != null) {" +
                "    playercont.style.setProperty('width', '100%', 'important');" +
                "}" +
                "var cdnplayer = document.getElementById('cdnplayer');" +
                "if (cdnplayer == null) {" +
                "    cdnplayer = document.getElementById('videoplayer');" +
                "}" +
                "if (cdnplayer != null) {" +
                "    cdnplayer.style.setProperty('width', '100%', 'important');" +
                "}" +
                "var restrplayer = document.getElementsByClassName(\"b-player__restricted\");" +
                "if (restrplayer.length > 0) {" +
                "    if (restrplayer[0]) {" +
                "        restrplayer[0].style.setProperty('width', '100%', 'important');" +
                "    }" +
                "}" + "})()"

        view?.evaluateJavascript(script2, null)

        val script2m = "javascript: (function() {" +
                "var elMain = document.getElementById('main');" +
                "if (elMain) {" +
                "    elMain.style.setProperty('padding', '0', 'important');" +
                "}" +
                "var bCont = document.getElementsByClassName('b-container');" +
                "if (bCont) {" +
                "    bCont[0].style.setProperty('width', 'unset', 'important');" +
                "}" +
                "var bContCol = document.getElementsByClassName('b-content__columns');" +
                "if (bContCol) {" +
                "    bContCol[0].style.setProperty('padding', '0', 'important');" +
                "    bCont[0].style.setProperty('padding', '0', 'important');" +
                "}" +
                "" + // remove advertisements
                "var bannerCont = document.getElementsByClassName('banner-container')[0];" +
                "if(bannerCont) {bannerCont.style.display = 'none'; bannerCont.parentElement.parentElement.style.height = '0px';}" +
                "document.body.classList.remove('active-brand');" +
                "function setPos(){" +
                "   var isChanged = false;" +
                "   for (var i = 0; i < document.body.childNodes.length; i++) {" +
                "       var node = document.body.childNodes[i];" +
                "       if(node.childNodes.length > 0 && node.style.position == 'fixed'){" +
                "           node.style.display = 'none'; " +
                "           isChanged = true; " +
                "           return;}" +
                "   } " +
                "   if(!isChanged){" +
                "       setTimeout(setPos, 1000);}" +
                "} " +
                "setTimeout(setPos, 1000);" +
                "})()"
        view?.evaluateJavascript(script2m, null)

        view?.evaluateJavascript(
            "var vkGroups = document.getElementById('vk_groups');\n" +
                    "if(vkGroups){\n" +
                    "    vkGroups.style.setProperty('display', 'none', 'important');\n" +
                    "}", null
        )

        // fix translators block width
        val script3 = "var translationsHint = document.getElementsByClassName('b-rgstats__help')[0];" +
                "if(translationsHint){" +
                "translationsHint.addEventListener('click', function (event) {" +
                "    var block = document.getElementsByClassName('tooltipster-base')[0];" +
                "    block.style.minWidth = 'unset';" +
                "    block.style.maxWidth = 'unset';" +
                "    block.style.left = 'unset';" +
                "    block.style.width = '100%';" +
                "    document.getElementsByClassName('tooltipster-arrow-bottom-right')[0].style.display = 'none';" +
                "});" +
                "}"
        view?.evaluateJavascript(script3, null)

        // hide telegram hint
        view?.evaluateJavascript("\$('#tg-info-block-exclusive-close').click()", null)

        // update watch later fragment
        view?.evaluateJavascript(
            "\$(document).ajaxComplete(function(event,request, settings){" +
                    "Android.updateWatchLater();" +
                    "});", null
        )

        val script4 = "setTimeout(function () {\n" +
                "    document.body.childNodes[0].style.setProperty('display', 'none', 'important');\n" +
                "    var iframeEls = document.getElementsByTagName('iframe');\n" +
                "    for (var j = 0; j < iframeEls.length; ++j) {\n" +
                "        var iframeEl = iframeEls[j];\n" +
                "\n" +
                "        if (iframeEl.id != 'pjsfrrscdnplayer') {\n" +
                "            iframeEl.parentNode.style.setProperty('display', 'none', 'important');\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    var aEls = document.getElementsByTagName('a');\n" +
                "    for (var j = 0; j < aEls.length; ++j) {\n" +
                "        var aEl = aEls[j];\n" +
                "\n" +
                "         if (aEl.href.match('help')) {\n" +
                "            aEl.style.setProperty('display', 'none', 'important');\n" +
                "            aEl.style.setProperty('height', '0px', 'important');\n" +
                "            aEl.style.setProperty('width', '0px', 'important');\n" +
                "        } else if (aEl.href.match('://')) {\n" +
                "            aEl.parentNode.style.setProperty('display', 'none', 'important');\n" +
                "            aEl.parentNode.style.setProperty('height', '0px', 'important');\n" +
                "            aEl.parentNode.style.setProperty('width', '0px', 'important');\n" +
                "        }\n" +
                "    }\n" +
                "}, 1500)"
        view?.evaluateJavascript(script4, null)

        val script5 = "var mediaElement;" +
                "mediaCheck();" +
                "document.onclick = function(){" +
                "    mediaCheck();" +
                "};" +
                "function mediaCheck(){" +
                "    for(var i = 0; i < document.getElementsByTagName('video').length; i++){" +
                "        var media = document.getElementsByTagName('video')[i];" +
                "        media.onplay = function(){" +
                "            mediaElement = media;" +
                "            JSOUT.mediaAction('true');" +
                "        };" +
                "        media.onpause = function(){" +
                "            mediaElement = media;" +
                "            JSOUT.mediaAction('false');" +
                "        };" +
                "    }" +
                "    for(var i = 0; i < document.getElementsByTagName('audio').length; i++){" +
                "        var media = document.getElementsByTagName('audio')[i];" +
                "        media.onplay = function(){" +
                "            mediaElement = media;" +
                "            JSOUT.mediaAction('true');" +
                "        };" +
                "        media.onpause = function(){" +
                "            mediaElement = media;" +
                "            JSOUT.mediaAction('false');" +
                "        };" +
                "    }" +
                "}"
        view?.evaluateJavascript(script5, null)

        // fix adguard empty blocks
        val script6 = "var mainElements = document.getElementsByClassName(\"b-content__main\")[0].childNodes\n" +
                "for (var i = 0; i < mainElements.length; ++i) {\n" +
                "    var el = mainElements[i];\n" +
                "    if (el.classList && el.classList.length > 0) {\n" +
                "        if (el.classList[0] == \"b-post__lastepisodeout\") {\n" +
                "            mainElements[i - 6].style.setProperty('height', '0px', 'important');\n" +
                "        }\n" +
                "\n" +
                "        if (el.classList[0] == \"b-post__rating_table\") {\n" +
                "            mainElements[i + 2].style.setProperty('height', '0px', 'important');\n" +
                "        }\n" +
                "    }\n" +
                "}"
        view?.evaluateJavascript(script6, null)

        if (film.autoswitch != null && film.autoswitch!!.isNotEmpty()) {
            view?.evaluateJavascript(film.autoswitch!!, null)
        }

        callback()

        super.onPageFinished(view, url)
    }

    private fun checkUrl(url: String): Boolean {
        return if (url == "https://t.me/hdrezka") {
            val linkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(linkIntent)
            true
        } else {
            false
        }
    }
}