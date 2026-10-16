import type {GuideTreeDto} from '../api/types';

export function treeLayout(tree: GuideTreeDto, proportional = true, requestedWidth = 760) {
    const width = Math.max(300, requestedWidth), left = 18, labelWidth = Math.min(300, width * 0.49);
    const branchWidth = width - labelWidth - left - 20;
    const children = tree.nodes.map(() => [] as number[]);
    const distances = tree.nodes.map(() => 0), depths = tree.nodes.map(() => 0);
    tree.nodes.forEach(node => {
        if (node.parentId !== null) {
            children[node.parentId].push(node.id);
            distances[node.id] = distances[node.parentId] + Math.max(0, node.length ?? 0);
            depths[node.id] = depths[node.parentId] + 1;
        }
    });
    const maximum = Math.max(...distances), scaled = proportional && maximum > 0;
    const units = scaled ? distances : depths, extent = Math.max(1e-12, ...units);
    const labelsPerLine = Math.max(10, Math.floor((labelWidth - 10) / 7));
    const wrap = (text: string) => {
        const chars = Array.from(text), lines: string[] = [];
        for (let i = 0; i < chars.length; i += labelsPerLine) lines.push(chars.slice(i, i + labelsPerLine).join(''));
        return lines;
    };
    const ys = tree.nodes.map(() => 0), labels = new Map<number, string[]>();
    let cursor = 14;
    const place = (index: number) => {
        const node = tree.nodes[index];
        if (node.sourceIndex !== null) {
            const lines = wrap(`${node.sourceIndex + 1}. ${node.name}`);
            const height = Math.max(30, lines.length * 16 + 12);
            labels.set(index, lines);
            ys[index] = cursor + height / 2;
            cursor += height;
        } else {
            children[index].forEach(place);
            ys[index] = (ys[children[index][0]] + ys[children[index].at(-1)!]) / 2;
        }
    };
    place(0);
    const nodes = tree.nodes.map(node => ({
        ...node,
        x: left + units[node.id] / extent * branchWidth,
        y: ys[node.id],
        lines: labels.get(node.id) ?? []
    }));
    const links = nodes.filter(node => node.parentId !== null).map(node => {
        const parent = nodes[node.parentId!];
        return {id: node.id, length: node.length!, path: `M${parent.x},${parent.y}V${node.y}H${node.x}`};
    });
    const scale = scaled ? 10 ** Math.floor(Math.log10(maximum / 4)) : null;
    return {
        width, height: cursor + (scaled ? 44 : 12), nodes, links, scaled,
        labelX: width - labelWidth, scale, scaleWidth: scale === null ? 0 : scale / maximum * branchWidth,
        negative: nodes.some(node => node.length !== null && node.length < 0)
    };
}
