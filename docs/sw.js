/* Offline cache for the rig.
 *
 * The page must work on a bench with no signal, but it also has to pick up new
 * versions without anyone clearing site data. So: the page itself is
 * network-first (fresh when online, cached when not), everything else is
 * cache-first (icons and the manifest never change).
 */
const CACHE = "calccase-v2";
const ASSETS = [
  "./", "./index.html", "./manifest.webmanifest",
  "./icon-192.png", "./icon-512.png", "./icon-maskable-512.png"
];

self.addEventListener("install", e => {
  e.waitUntil(
    caches.open(CACHE)
      .then(c => c.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys()
      .then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

const isPage = req => {
  if (req.mode === "navigate") return true;
  const p = new URL(req.url).pathname;
  return p.endsWith("/") || p.endsWith("/index.html");
};

self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET") return;
  if (new URL(req.url).origin !== location.origin) return;   // let fonts fail on their own

  if (isPage(req)) {
    e.respondWith(
      fetch(req)
        .then(res => {
          if (res && res.ok) {
            const copy = res.clone();
            caches.open(CACHE).then(c => c.put("./index.html", copy));
          }
          return res;
        })
        .catch(() => caches.match("./index.html").then(r => r || caches.match("./")))
    );
    return;
  }

  e.respondWith(
    caches.match(req).then(hit =>
      hit || fetch(req).then(res => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE).then(c => c.put(req, copy));
        }
        return res;
      })
    )
  );
});
