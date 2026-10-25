#!/bin/sh
# Keep portal backends available to the HA proxy and peer, but not LAN clients.
set -eu

iptables_bin=/usr/sbin/iptables
chain=SRGH_PORTAL_GUARD

"$iptables_bin" -N "$chain" 2>/dev/null || true
"$iptables_bin" -F "$chain"

# Local HA nginx/monitoring containers and the two application nodes.
for source in 127.0.0.0/8 172.19.0.0/16 192.168.1.76/32 192.168.1.77/32 192.168.1.78/32 192.168.1.249/32; do
  "$iptables_bin" -A "$chain" -s "$source" -j RETURN
done
"$iptables_bin" -A "$chain" -j DROP

# Portal backends: work UI/API, worklog, management UI/API, register,
# account, server, vendor, rental, install and notion.
"$iptables_bin" -D INPUT -p tcp -m multiport --dports 13006,13011,3016,3017,3018,3019,3024 -j "$chain" 2>/dev/null || true
"$iptables_bin" -D INPUT -p tcp -m multiport --dports 13101,18101,13006,13011,3016,3017,3018,3019,3024 -j "$chain" 2>/dev/null || true
"$iptables_bin" -C INPUT -p tcp -m multiport --dports 13000,13007,18000,13101,18101,13006,13011,3016,3017,3018,3019,3024 -j "$chain" 2>/dev/null ||
  "$iptables_bin" -I INPUT 1 -p tcp -m multiport --dports 13000,13007,18000,13101,18101,13006,13011,3016,3017,3018,3019,3024 -j "$chain"

# VIP port 3011 serves account; the node-address port 3011 remains Orchestrator.
"$iptables_bin" -C INPUT -d 192.168.1.76/32 -p tcp --dport 3011 -j "$chain" 2>/dev/null ||
  "$iptables_bin" -I INPUT 1 -d 192.168.1.76/32 -p tcp --dport 3011 -j "$chain"
