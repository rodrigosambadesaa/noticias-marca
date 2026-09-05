# Noticias MARCA
Independent Android RSS reader for MARCA.

RSS: https://e00-xlk-ue-marca.uecdn.es/rss/googlenews/portada.xml

Search, refresh, sharing, article view, offline cache, passive NetworkObserver and full connectivity diagnostics. Complete requested gist is vendored under third_party/connectivity.

Remote loading uses a cheap `ConnectivityAndInternetAccess.isConnected()`/capabilities guard before initial load, refresh, retries, pagination and image requests. When the guard passes, the RSS request runs directly because the feed response is the definitive service check. HTTP responses are reported as feed/service failures without a redundant general probe. Only ambiguous transport failures (DNS, connect/read timeout, TLS or equivalent) trigger the Gist's active diagnostic afterward, distinguishing a feed outage from general connectivity loss. Offline and failed loads fall back to the cached news.

Validation: `./gradlew testDebugUnitTest lintRelease assembleRelease`.

This is not an official MARCA application.
