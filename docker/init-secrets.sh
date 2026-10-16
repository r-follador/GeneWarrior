#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
umask 077
mkdir -p .secrets
chmod 700 .secrets
if [ ! -e .secrets/db-password ]; then
    (set -C; od -An -N32 -tx1 /dev/urandom | tr -d ' \n' > .secrets/db-password)
    # The parent is private on the host; both non-root container UIDs need the mount.
    chmod 444 .secrets/db-password
    printf '%s\n' 'Created .secrets/db-password. Existing database credentials were not changed.'
else
    printf '%s\n' 'Keeping existing .secrets/db-password.'
fi
