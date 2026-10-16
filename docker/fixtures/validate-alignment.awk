/^>/ {
    id = substr($1, 2)
    if (FILENAME == ARGV[1]) {
        if (id in source) exit 1
        source[id] = ""
    } else {
        if (!(id in source) || id in aligned) exit 1
        aligned[id] = ""
    }
    next
}
{
    gsub(/[[:space:]]/, "")
    if (FILENAME == ARGV[1]) source[id] = source[id] toupper($0)
    else aligned[id] = aligned[id] toupper($0)
}
END {
    count = 0
    for (id in source) {
        if (!(id in aligned)) exit 1
        if (width && length(aligned[id]) != width) exit 1
        width = length(aligned[id])
        ungapped = aligned[id]
        gsub(/-/, "", ungapped)
        if (source[id] != ungapped) exit 1
        count++
    }
    if (count < 2 || width < 1) exit 1
}
