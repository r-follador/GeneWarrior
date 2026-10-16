import {strToU8, zipSync} from 'fflate';
import type {AlignmentDto, SequenceDto} from '../api/types';
import {clustalFiles, fasta, filenameStem} from './formats';

export function downloadBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.append(link);
    link.click();
    link.remove();
    // Leave time for the browser to consume the URL before releasing its data.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export function downloadFasta(name: string, sequences: SequenceDto[]) {
    if (!sequences.length) return;
    downloadBlob(new Blob([fasta(sequences)], {type: 'text/plain;charset=utf-8'}), `${filenameStem(name)}.fasta`);
}

export function downloadClustal(name: string, alignments: AlignmentDto[]) {
    const files = clustalFiles(alignments);
    if (!files.length) return;
    if (files.length === 1) {
        downloadBlob(new Blob([files[0].content], {type: 'text/plain;charset=utf-8'}), files[0].name);
    } else {
        const archive = zipSync(Object.fromEntries(files.map(file => [file.name, strToU8(file.content)])), {level: 0});
        downloadBlob(new Blob([new Uint8Array(archive)], {type: 'application/zip'}), `${filenameStem(name)}-alignments.zip`);
    }
}
