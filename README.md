# Noticias MARCA
Independent Android RSS reader for MARCA.

RSS: https://e00-xlk-ue-marca.uecdn.es/rss/googlenews/portada.xml

Search, refresh, sharing, article view, offline cache, passive NetworkObserver and full connectivity diagnostics. Complete requested gist is vendored under third_party/connectivity.

Remote loading uses the cheap conjunction `ConnectivityAndInternetAccess.isConnected()` **and** `hasPhysicalNetwork()` before initial load, refresh, retries, pagination, article WebView loads and image requests. This prevents a VPN-only/AdGuard network from being mistaken for a usable physical connection. When the guard passes, the RSS request runs directly because the feed response is the definitive service check. HTTP responses are reported as feed/service failures without a redundant general probe. Only ambiguous transport failures (DNS, connect/read timeout, TLS or equivalent) trigger the Gist's active diagnostic afterward, distinguishing a feed outage from general connectivity loss. Offline and failed loads fall back to the cached news.

Validation: `./gradlew testDebugUnitTest lintRelease assembleRelease`.

Orientation changes restore the cached list, pagination state and `RecyclerView` scroll position without starting another RSS download.

Manual connectivity matrix: test normal Wi-Fi, Wi-Fi with AdGuard VPN, VPN-only with Wi-Fi/mobile/Ethernet disabled, recovery after re-enabling a physical transport, no VPN/no network, and captive portal. The VPN-only case must remain offline without starting a request or spinner; recovery must be observed without restarting the Activity.

This is not an official MARCA application.
