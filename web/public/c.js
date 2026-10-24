// Renders a shared ReelVault collection from its public Firestore snapshot at shared/{shareId}.
// Uses the Firestore REST API directly (public read per firestore.rules), so no SDK or config is
// bundled; the project id comes from Hosting's reserved /__/firebase/init.json.
(function () {
  "use strict";

  // Same gradients and monograms as the app (PlatformStyle.kt), for frames without a usable image.
  var PLATFORMS = {
    TIKTOK: { name: "TikTok", mark: "TT", colors: ["#25F4EE", "#111111", "#FE2C55"] },
    INSTAGRAM: { name: "Instagram", mark: "IG", colors: ["#F58529", "#DD2A7B", "#8134AF"] },
    YOUTUBE: { name: "YouTube", mark: "YT", colors: ["#FF3D3D", "#B0001E"] },
    FACEBOOK: { name: "Facebook", mark: "f", colors: ["#3B8BFF", "#0A4FB3"] },
    X: { name: "X", mark: "X", colors: ["#3A3F47", "#0E0F11"] },
    PINTEREST: { name: "Pinterest", mark: "P", colors: ["#FF3B5C", "#A3001A"] },
    XIAOHONGSHU: { name: "RED", mark: "RED", colors: ["#FF6A80", "#E0102F"] },
    OTHER: { name: "Web", mark: "↗", colors: ["#E8475F", "#6B5CE7"] }
  };
  var DEFAULT_COLOR = "#E8475F";

  var $ = function (id) { return document.getElementById(id); };

  function shareIdFromPath() {
    var match = location.pathname.match(/^\/c\/([A-Za-z0-9]{6,40})\/?$/);
    return match ? match[1] : null;
  }

  function projectId() {
    return fetch("/__/firebase/init.json")
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (cfg) {
        if (cfg && cfg.projectId) return cfg.projectId;
        // Fallback for <project>.web.app when init.json isn't reachable.
        return location.hostname.replace(/\.(web\.app|firebaseapp\.com)$/, "");
      });
  }

  // Firestore REST values are typed wrappers ({stringValue: "..."}); unwrap them.
  function unwrap(v) {
    if (!v) return null;
    if ("stringValue" in v) return v.stringValue;
    if ("integerValue" in v) return Number(v.integerValue);
    if ("doubleValue" in v) return v.doubleValue;
    if ("booleanValue" in v) return v.booleanValue;
    if ("timestampValue" in v) return v.timestampValue;
    if ("arrayValue" in v) return (v.arrayValue.values || []).map(unwrap);
    if ("mapValue" in v) return unwrapFields(v.mapValue.fields || {});
    return null;
  }
  function unwrapFields(fields) {
    var out = {};
    Object.keys(fields).forEach(function (k) { out[k] = unwrap(fields[k]); });
    return out;
  }

  function hexFromArgb(argb) {
    if (typeof argb !== "number") return DEFAULT_COLOR;
    return "#" + (argb & 0xffffff).toString(16).padStart(6, "0");
  }

  // White or ink text on the collection color, whichever reads better (WCAG relative luminance).
  function textOn(hex) {
    var c = [1, 3, 5].map(function (i) {
      var s = parseInt(hex.substr(i, 2), 16) / 255;
      return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    });
    var lum = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    return lum > 0.4 ? "#17151C" : "#FFFFFF";
  }

  function safeHttpUrl(url) {
    try {
      var u = new URL(url);
      return u.protocol === "https:" || u.protocol === "http:" ? u.href : null;
    } catch (e) { return null; }
  }

  function el(tag, cls, text) {
    var node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text) node.textContent = text;
    return node;
  }

  function frameFor(item) {
    var href = safeHttpUrl(item.url);
    if (!href) return null;
    var platform = PLATFORMS[item.platform] || PLATFORMS.OTHER;
    var text = item.title || item.caption || "";

    var li = el("li", "frame");
    var a = el("a");
    a.href = href;
    a.target = "_blank";
    a.rel = "noopener noreferrer";
    a.setAttribute("aria-label", (text ? text + ", " : "") + "on " + platform.name);

    var shot = el("div", item.platform === "YOUTUBE" ? "shot youtube" : "shot");
    shot.style.background = platform.colors[platform.colors.length - 1];
    shot.appendChild(el("span", "mark", platform.mark));
    var thumb = item.thumbnailUrl && safeHttpUrl(item.thumbnailUrl);
    if (thumb) {
      var img = el("img");
      img.alt = "";
      img.loading = "lazy";
      // Platform CDNs often refuse hotlinked images that carry a referrer; signed URLs can also expire.
      img.referrerPolicy = "no-referrer";
      img.onerror = function () { img.remove(); };
      img.src = thumb;
      shot.appendChild(img);
    }
    shot.appendChild(el("span", "pill", platform.name));
    a.appendChild(shot);
    if (text) a.appendChild(el("p", "caption", text));
    if (item.authorName) a.appendChild(el("p", "author", item.authorName));
    li.appendChild(a);
    return li;
  }

  function showNotice(title, body) {
    $("name").textContent = title;
    $("count").textContent = "";
    $("frames").replaceChildren();
    $("frames").removeAttribute("aria-busy");
    var notice = $("notice");
    notice.textContent = body;
    notice.hidden = false;
    document.title = title + " · ReelVault";
  }

  function render(data) {
    var color = hexFromArgb(data.colorArgb);
    var cover = $("cover");
    cover.style.setProperty("--cover", color);
    cover.style.setProperty("--cover-text", textOn(color));
    $("emoji").textContent = data.emoji || "";
    $("name").textContent = data.name || "Shared collection";
    var items = Array.isArray(data.items) ? data.items : [];
    $("count").textContent = (items.length === 1 ? "1 video" : items.length + " videos") + ", shared with ReelVault";
    document.title = (data.name || "Shared collection") + " · ReelVault";

    var list = $("frames");
    list.replaceChildren();
    items.map(frameFor).forEach(function (li) { if (li) list.appendChild(li); });
    list.removeAttribute("aria-busy");
    if (!list.children.length) showNotice(data.name || "Shared collection", "This collection is empty right now.");
  }

  var shareId = shareIdFromPath();
  if (!shareId) {
    showNotice("Link not found", "Check that the whole link was copied.");
    return;
  }
  projectId()
    .then(function (project) {
      var url = "https://firestore.googleapis.com/v1/projects/" + encodeURIComponent(project) +
        "/databases/(default)/documents/shared/" + encodeURIComponent(shareId);
      return fetch(url);
    })
    .then(function (r) {
      if (r.status === 404 || r.status === 403) return null;
      if (!r.ok) throw new Error("HTTP " + r.status);
      return r.json();
    })
    .then(function (doc) {
      if (!doc) {
        showNotice("No longer shared", "The person who shared this collection turned the link off.");
        return;
      }
      render(unwrapFields(doc.fields || {}));
    })
    .catch(function () {
      showNotice("Couldn't load this collection", "Check your connection, then reload the page.");
    });
})();
