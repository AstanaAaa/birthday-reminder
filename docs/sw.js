/**
 * 离线缓存。
 * 策略：缓存优先 + 网络回填。第一次打开后，断网也能用。
 * 改了文件记得把 VERSION 加一，否则用户会一直看到旧版本。
 */
const VERSION = 'birthday-reminder-v1';

const ASSETS = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './ics.js',
  './manifest.webmanifest',
  './icon-192.png',
  './icon-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(VERSION)
      .then((cache) => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
      .catch(() => {
        /* 某个资源拿不到也不阻塞安装 */
      }),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((key) => key !== VERSION).map((key) => caches.delete(key)),
      ))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request)
        .then((response) => {
          // 只缓存同源的静态资源
          if (response.ok && new URL(request.url).origin === self.location.origin) {
            const copy = response.clone();
            caches.open(VERSION).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => caches.match('./index.html'));
    }),
  );
});
