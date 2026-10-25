#!/bin/sh
set -eu

live=/opt/SRGH_IT/nev/nev.jar
backup=/opt/SRGH_IT/nev/nev.jar.before-external-group-button-position-20261005
cp -p "$live" "$backup"
install -m 644 -o sarangit -g sarangit /tmp/nev-external-group-button-position-20261005.jar "$live"
systemctl restart SRGH_IT-nev.service

ready=0
attempt=0
while [ "$attempt" -lt 20 ]; do
    if curl -fsS --max-time 3 http://127.0.0.1:13013/it/ >/dev/null 2>&1; then
        ready=1
        break
    fi
    attempt=$((attempt + 1))
    sleep 2
done
if [ "$ready" -ne 1 ]; then
    cp -p "$backup" "$live"
    systemctl restart SRGH_IT-nev.service
    exit 1
fi
systemctl is-active SRGH_IT-nev.service
