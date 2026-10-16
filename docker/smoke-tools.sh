#!/bin/sh
set -eu
umask 077
work=$(mktemp -d /tmp/genewarrior-smoke.XXXXXX)
trap 'rm -rf "$work"' EXIT HUP INT TERM
export HOME="$work/home" TMPDIR="$work/tmp" PATH=/usr/bin:/bin
export TMP_4_TCOFFEE="$TMPDIR"
mkdir -p "$HOME" "$TMPDIR"
cd "$work"
fixtures=/opt/genewarrior/fixtures
if command -v muscle >/dev/null 2>&1; then
    printf '%s\n' 'Unexpected MUSCLE dependency in runtime' >&2
    exit 1
fi
t_coffee -version
for kind in dna protein; do
    cp "$fixtures/$kind.fasta" "$kind.fasta"
    /usr/bin/time -f "$kind: %e seconds, %M KiB peak RSS" \
        timeout -k 5 60 t_coffee -seq "$kind.fasta" -type "$kind" \
        -method slow_pair,lalign_id_pair -output fasta_aln -outfile "$kind.aligned.fasta" \
        -tree_mode nj -newtree "$kind.nwk" -n_core 1 -quiet > "$kind.stdout" 2> "$kind.stderr" || {
            cat "$kind.stderr" >&2
            exit 1
        }
    awk -f "$fixtures/validate-alignment.awk" "$kind.fasta" "$kind.aligned.fasta"
    test -s "$kind.nwk"
    grep -q ';' "$kind.nwk"
    tail -1 "$kind.stderr"
done
test -s /etc/primer3_config/stack.dh
/usr/bin/time -f 'primer3: %e seconds, %M KiB peak RSS' \
    timeout -k 5 30 primer3_core < "$fixtures/primer3.txt" > primer3.out
awk -F= '
    /^PRIMER_ERROR=/ { bad = 1 }
    /^PRIMER_PAIR_NUM_RETURNED=/ { pairs = $2 + 0 }
    /^PRIMER_LEFT_0_SEQUENCE=/ { left = length($2) }
    /^PRIMER_RIGHT_0_SEQUENCE=/ { right = length($2) }
    /^PRIMER_PAIR_0_PRODUCT_SIZE=/ { product = $2 + 0 }
    END { if (bad || pairs < 1 || left < 18 || right < 18 || product < 100 || product > 250) exit 1 }
' primer3.out || { cat primer3.out >&2; exit 1; }
dpkg-query -W -f='${Package} ${Version}\n' t-coffee primer3
printf '%s\n' 'Tool fixtures passed (DNA/protein residue preservation; thermodynamic primer pair).'
