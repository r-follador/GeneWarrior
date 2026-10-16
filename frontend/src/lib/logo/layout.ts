import type {SequenceLogoDto} from '../api/types';

const dnaColors: Record<string, string> = {A: '#248743', C: '#2778ba', G: '#b58900', T: '#cc3b3b'};
const proteinGroups = [
    {letters: 'GSTYC', color: '#248743'}, {letters: 'NQ', color: '#8362a7'},
    {letters: 'KRH', color: '#2778ba'}, {letters: 'DE', color: '#cc3b3b'},
    {letters: 'AVLIPWFM', color: '#38484c'}
];

export function residueColor(kind: SequenceLogoDto['kind'], residue: string) {
    return kind === 'DNA' ? dnaColors[residue] : proteinGroups.find(group => group.letters.includes(residue))?.color ?? '#38484c';
}

export function logoLayout(logo: SequenceLogoDto, columnsPerLine = 40) {
    const maximum = logo.mode === 'FREQUENCY' ? 1 : Math.log2(logo.kind === 'DNA' ? 4 : 20);
    const lineColumns = Math.max(1, Math.min(80, Math.floor(columnsPerLine)));
    return Array.from({length: Math.ceil(logo.columns.length / lineColumns)}, (_, line) => {
        const columns = logo.columns.slice(line * lineColumns, (line + 1) * lineColumns);
        return {
            width: 60 + lineColumns * 22, height: 212, maximum,
            ticks: [0, maximum / 2, maximum],
            columns: columns.map((column, index) => {
                let bottom = 168;
                const symbols = [...column.symbols].sort((a, b) => a.frequency - b.frequency || a.residue.localeCompare(b.residue));
                return {
                    ...column, x: 48 + index * 22,
                    coverage: column.observations / logo.sequenceCount,
                    glyphs: symbols.flatMap(symbol => {
                        const value = logo.mode === 'FREQUENCY' ? symbol.frequency : symbol.bits;
                        if (value <= 1e-12) return [];
                        const height = value / maximum * 140;
                        bottom -= height;
                        return [{...symbol, y: bottom, height, color: residueColor(logo.kind, symbol.residue)}];
                    })
                };
            })
        };
    });
}
