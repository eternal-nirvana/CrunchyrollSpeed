package com.example

import java.util.Locale

/**
 * PlayerJs provides JavaScript snippets injected into Crunchyroll web pages.
 *
 * It ensures the video playback speed is applied and forcefully retained even if:
 * 1. The HTML5 player loads asynchronously after initial page load (via MutationObserver).
 * 2. Ads finish or new episodes load (via loadedmetadata, play, playing events).
 * 3. The Crunchyroll internal player code attempts to reset the playbackRate back to 1.0 (via ratechange listener).
 */
object PlayerJs {

    /**
     * Builds the complete injection script that sets up the observer, attaches event
     * listeners to any <video> tag found, and exposes window.setCrSpeed(rate).
     */
    fun getInjectionScript(initialSpeed: Float): String {
        val speedStr = String.format(Locale.US, "%.2f", initialSpeed)
        return """
            (function() {
                // Store desired speed globally
                window.__crSpeed = $speedStr;

                // Function to apply the speed safely to a single video element
                function applySpeedToVideo(v) {
                    if (!v) return;
                    try {
                        if (Math.abs(v.playbackRate - window.__crSpeed) > 0.01) {
                            v.playbackRate = window.__crSpeed;
                            v.defaultPlaybackRate = window.__crSpeed;
                        }
                    } catch (e) {
                        console.error('CR Speed: error setting playbackRate', e);
                    }
                }

                // Bind persistent rate-enforcing event listeners to a video element
                function setupVideo(v) {
                    if (!v || v.__crSpeedBound) return;
                    v.__crSpeedBound = true;

                    applySpeedToVideo(v);

                    var events = ['ratechange', 'loadedmetadata', 'play', 'playing', 'canplay', 'timeupdate'];
                    events.forEach(function(evt) {
                        v.addEventListener(evt, function() {
                            applySpeedToVideo(v);
                        }, true);
                    });
                }

                // Scan all video elements currently attached to the DOM
                function scanAndApply() {
                    try {
                        var videos = document.querySelectorAll('video');
                        for (var i = 0; i < videos.length; i++) {
                            setupVideo(videos[i]);
                            applySpeedToVideo(videos[i]);
                        }
                    } catch (e) {
                        console.error('CR Speed: scan error', e);
                    }
                }

                // Initial scan
                scanAndApply();

                // Setup MutationObserver to observe when player attaches <video> dynamically
                if (!window.__crSpeedObserver) {
                    try {
                        window.__crSpeedObserver = new MutationObserver(function(mutations) {
                            scanAndApply();
                        });
                        var targetNode = document.documentElement || document.body;
                        if (targetNode) {
                            window.__crSpeedObserver.observe(targetNode, {
                                childList: true,
                                subtree: true
                            });
                        }
                    } catch (e) {
                        console.error('CR Speed: MutationObserver error', e);
                    }
                }

                // Periodic safety poll (every 1 second) in case site scripts swallow events
                if (!window.__crSpeedInterval) {
                    window.__crSpeedInterval = setInterval(scanAndApply, 1000);
                }

                // Global function callable from Kotlin via evaluateJavascript
                window.setCrSpeed = function(newRate) {
                    window.__crSpeed = Number(newRate);
                    scanAndApply();
                };
            })();
        """.trimIndent()
    }

    /**
     * Builds the snippet to update the speed on a page where the script is already injected.
     */
    fun getSetSpeedScript(speed: Float): String {
        val speedStr = String.format(Locale.US, "%.2f", speed)
        return """
            (function() {
                if (typeof window.setCrSpeed === 'function') {
                    window.setCrSpeed($speedStr);
                } else {
                    ${getInjectionScript(speed)}
                }
            })();
        """.trimIndent()
    }
}
