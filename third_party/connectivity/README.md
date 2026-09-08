# ConnectivityAndInternetAccess
Upstream source: https://gist.github.com/rodrigosambadesaa/729cca29a031fef4e2f15751863b655f
Pinned revision: c0ffd1214abb9c8195c00c4247d5c5068cad22df
Compiled copy changes the package declaration and includes the additive
`hasPhysicalNetwork(Context)` helper required to distinguish a VPN-only network
from Wi-Fi, cellular, or Ethernet connectivity.
