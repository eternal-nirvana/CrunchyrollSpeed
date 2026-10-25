"""
CR Speed for Laptop / Desktop (Windows)
Dedicated Crunchyroll Player with Speed Control Pill & Hotkeys
"""
import sys
import webview

SPEED_INJECTION_JS = """
(function() {
    window.__crSpeed = 1.0;
    
    function applySpeed(v) {
        if (!v) return;
        try {
            if (Math.abs(v.playbackRate - window.__crSpeed) > 0.01) {
                v.playbackRate = window.__crSpeed;
                v.defaultPlaybackRate = window.__crSpeed;
            }
        } catch(e) {}
    }

    function setupVideo(v) {
        if (!v || v.__crSpeedBound) return;
        v.__crSpeedBound = true;
        applySpeed(v);
        ['ratechange', 'loadedmetadata', 'play', 'playing', 'canplay', 'timeupdate'].forEach(function(evt) {
            v.addEventListener(evt, function() { applySpeed(v); }, true);
        });
    }

    function scan() {
        var videos = document.querySelectorAll('video');
        for (var i = 0; i < videos.length; i++) {
            setupVideo(videos[i]);
            applySpeed(videos[i]);
        }
    }

    scan();

    if (!window.__crSpeedObserver) {
        window.__crSpeedObserver = new MutationObserver(scan);
        var target = document.documentElement || document.body;
        if (target) window.__crSpeedObserver.observe(target, { childList: true, subtree: true });
    }

    if (!window.__crSpeedInterval) {
        window.__crSpeedInterval = setInterval(scan, 1000);
    }

    window.setCrSpeed = function(speed) {
        window.__crSpeed = Number(speed);
        scan();
        updatePill(speed);
    };

    function injectPill() {
        if (document.getElementById('cr-speed-pill-root')) return;
        
        var pill = document.createElement('div');
        pill.id = 'cr-speed-pill-root';
        pill.style.cssText = 'position:fixed; top:24px; right:32px; z-index:999999; font-family:sans-serif; user-select:none;';
        
        pill.innerHTML = `
            <div id="cr-speed-badge" style="background:rgba(26,26,36,0.92); border:1.5px solid #F47521; color:#fff; padding:6px 14px; border-radius:20px; font-weight:bold; font-size:13px; cursor:pointer; box-shadow:0 4px 12px rgba(0,0,0,0.5); display:flex; align-items:center; gap:6px;">
                <span>⚡</span><span id="cr-speed-val">1.00x</span>
            </div>
            <div id="cr-speed-menu" style="display:none; position:absolute; right:0; top:36px; background:#161622; border:1px solid rgba(244,117,33,0.4); border-radius:12px; padding:12px; width:220px; box-shadow:0 8px 24px rgba(0,0,0,0.6); color:#fff;">
                <div style="font-size:12px; color:#aaa; margin-bottom:8px; font-weight:bold;">Playback Speed</div>
                <div style="display:grid; grid-template-columns:repeat(3, 1fr); gap:6px; margin-bottom:10px;">
                    <button class="cr-btn" data-speed="1.0">1.0x</button>
                    <button class="cr-btn" data-speed="1.5">1.5x</button>
                    <button class="cr-btn" data-speed="1.75">1.75x</button>
                    <button class="cr-btn" data-speed="2.0">2.0x</button>
                    <button class="cr-btn" data-speed="2.25">2.25x</button>
                    <button class="cr-btn" data-speed="2.5">2.5x</button>
                </div>
                <div style="display:flex; gap:6px; align-items:center;">
                    <input id="cr-custom-input" type="number" step="0.05" min="0.25" max="4.0" placeholder="Custom" style="width:100%; background:#252535; border:1px solid #444; color:#fff; border-radius:6px; padding:4px 8px; font-size:12px;">
                    <button id="cr-apply-btn" style="background:#F47521; border:none; color:#fff; font-weight:bold; border-radius:6px; padding:5px 10px; cursor:pointer; font-size:12px;">Set</button>
                </div>
                <div style="margin-top:10px; font-size:10px; color:#888; text-align:center;">Hotkeys: [ / ] adjust, \\ reset</div>
            </div>
        `;
        document.body.appendChild(pill);

        var badge = document.getElementById('cr-speed-badge');
        var menu = document.getElementById('cr-speed-menu');
        badge.onclick = function() {
            menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
        };

        var btns = pill.querySelectorAll('.cr-btn');
        btns.forEach(function(b) {
            b.style.cssText = 'background:#2A2A3A; border:1px solid #444; color:#fff; border-radius:6px; padding:4px 0; cursor:pointer; font-size:11px;';
            b.onclick = function() {
                var s = parseFloat(this.getAttribute('data-speed'));
                window.setCrSpeed(s);
                menu.style.display = 'none';
            };
        });

        document.getElementById('cr-apply-btn').onclick = function() {
            var val = parseFloat(document.getElementById('cr-custom-input').value);
            if (!isNaN(val) && val >= 0.25 && val <= 4.0) {
                window.setCrSpeed(val);
                menu.style.display = 'none';
            }
        };

        window.addEventListener('keydown', function(e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            if (e.key === ']') {
                window.setCrSpeed(Math.min(4.0, (window.__crSpeed + 0.25)));
            } else if (e.key === '[') {
                window.setCrSpeed(Math.max(0.25, (window.__crSpeed - 0.25)));
            } else if (e.key === '\\\\') {
                window.setCrSpeed(1.0);
            }
        });
    }

    function updatePill(speed) {
        var el = document.getElementById('cr-speed-val');
        if (el) el.innerText = Number(speed).toFixed(2) + 'x';
    }

    setInterval(injectPill, 1500);
})();
"""

def on_loaded(window):
    window.evaluate_js(SPEED_INJECTION_JS)

def main():
    window = webview.create_window(
        title="CR Speed - Crunchyroll Desktop",
        url="https://www.crunchyroll.com",
        width=1280,
        height=800,
        min_size=(800, 600)
    )
    window.events.loaded += on_loaded
    webview.start(private_mode=False)

if __name__ == "__main__":
    main()
