import {expect, it} from 'vitest';
import {get} from 'svelte/store';
import type {GuideTreeDto, ProjectDto} from '../api/types';
import {parseGuideTree, treeNewick, treeEligible} from './model';
import {treeLayout} from './layout';
import {parseProject} from '../project/projectValidation';
import {createProjectStore, exampleProject} from '../stores/projectStore';
import {exportEntries} from '../export/formats';

export function fixture(): GuideTreeDto {
    const names = ["O'Brien <sample>", 'Duplicate', 'Duplicate'];
    return {
        id: 'tree-test',
        name: 'Example guide tree',
        sourceAlignmentId: 'alignment',
        kind: 'DNA',
        version: '13.41',
        method: 'tcoffee-nj-guide-v1',
        sources: names.map(name => ({name, sha256: 'a'.repeat(64)})),
        nodes: [
            {id: 0, parentId: null, name: '', sourceIndex: null, length: null},
            ...names.map((name, index) => ({id: index + 1, parentId: 0, name, sourceIndex: index, length: index / 10}))]
    };
}

it('validates a multifurcating tree and rejects cycles, bad lengths and duplicate leaves', () => {
    expect(parseGuideTree(fixture())).toEqual(fixture());
    for (const node of [{parentId: 1}, {length: NaN}, {sourceIndex: 2}, {name: '<script>bad</script>'}]) {
        const tree = fixture();
        Object.assign(tree.nodes[1], node);
        expect(() => parseGuideTree(tree)).toThrow();
    }
});
it('keeps topology, signed lengths and quoted names in Newick', () => {
    const tree = fixture();
    tree.nodes[1].length = -0.05;
    expect(treeNewick(tree)).toBe("('O''Brien <sample>':-0.05,'Duplicate':0.1,'Duplicate':0.2);\n");
});
it('lays out non-overlapping wrapped labels without mutating data, including zero and negative branches', () => {
    const tree = fixture();
    tree.nodes[1].name = 'Long name '.repeat(20);
    tree.sources[0].name = tree.nodes[1].name;
    const before = structuredClone(tree);
    for (const width of [300, 760]) {
        const layout = treeLayout(tree, true, width);
        expect(layout.width).toBe(width);
        expect(layout.links).toHaveLength(3);
        const leaves = layout.nodes.filter(node => node.sourceIndex !== null);
        expect(leaves[1].y - leaves[0].y).toBeGreaterThan(leaves[0].lines.length * 8);
        expect(layout.nodes.every(node => Number.isFinite(node.x) && Number.isFinite(node.y))).toBe(true);
    }
    expect(tree).toEqual(before);
    tree.nodes.forEach(node => {
        if (node.length !== null) node.length = 0;
    });
    expect(treeLayout(tree).scaled).toBe(false);
    tree.nodes[1].length = -0.1;
    expect(treeLayout(tree).negative).toBe(true);
    expect(treeLayout(tree).scale).toBeNull();
});
it('persists, deduplicates, orders, exports and removes standalone tree cards', () => {
    const project: ProjectDto = {
        name: 'Trees',
        sequences: [],
        alignments: [],
        trees: [fixture()],
        entryOrder: ['tree-test']
    };
    const store = createProjectStore(parseProject(project));
    store.addTreeAfter('missing', fixture());
    expect(get(store).trees).toHaveLength(1);
    expect(exportEntries(get(store), [fixture().id])[0].type).toBe('tree');
    store.removeEntries([fixture().id]);
    expect(get(store).trees).toHaveLength(0);
    expect(get(store).entryOrder).toEqual([]);
});
it('limits the guide tree to supported, equal-length alignment rows', () => {
    const alignment = exampleProject().alignments[0];
    expect(treeEligible(alignment)).toBe(true);
    expect(treeEligible({...alignment, sequences: alignment.sequences.slice(0, 1)})).toBe(false);
    expect(treeEligible({...alignment, sequences: alignment.sequences.slice(0, 2)})).toBe(false);
    expect(treeEligible({
        ...alignment,
        sequences: [{name: 'One', value: 'XX'}, {name: 'Two', value: 'AA'}, {name: 'Three', value: 'AA'}]
    })).toBe(false);
});
