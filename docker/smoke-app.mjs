import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const base = process.env.GENEWARRIOR_FRONTEND_URL ?? 'http://127.0.0.1:8081';

async function request(path, status = 200, init = {}) {
    const response = await fetch(new URL(path, base), {...init, signal: AbortSignal.timeout(15_000)});
    assert.equal(response.status, status, `${init.method ?? 'GET'} ${path}`);
    return response;
}

const post = body => ({method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)});

for (const path of ['/actuator/health/liveness', '/actuator/health/readiness']) {
    assert.deepEqual(await (await request(path)).json(), {status: 'UP'});
}
const index = await request('/?project=smoke');
assert.match(index.headers.get('content-security-policy'), /script-src 'self';/);
assert.equal(index.headers.get('x-frame-options'), 'DENY');
const html = await index.text();
assert.ok(!html.includes('jquery'));
const assets = [...html.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g)].map(match => match[1]);
assert.ok(assets.length >= 2, 'Missing compiled frontend assets');
for (const asset of assets) await request(asset);
for (const path of ['/api/not-found', '/assets/not-found.js', '/js/dnatools.js', '/actuator/env',
    '/actuator/configprops', '/sharedProjectEntities', '/toolJobEntities', '/projectEntities']) {
    await request(path, 404);
}
for (const path of ['/dnatoolsServlet', '/dnatoolsServletDownload', '/readProject']) await request(path, 404, post({}));
await request('/api/alignments', 400, post({}));
await request('/api/primers', 400, post({}));
await request('/api/trees', 400, post({}));
const capabilities = await (await request('/api/tools/tcoffee')).json();
assert.equal(capabilities.available, true, 'T-Coffee probe failed');
for (const [kind, values] of [
    ['DNA', ['ACGTACGTACGTACGT', 'ACGTACGTACGT', 'ACGTACGTACGTACGTACGT']],
    ['PROTEIN', ['MQIFVKTLTGKTITLEVE', 'MQIFVKTLTGKTLEVE', 'MQIFVKTLTGKTITLEVE']]
]) {
    const body = {
        requestId: crypto.randomUUID(),
        sequences: values.map((sequence, index) => ({
            id: `row${index}`,
            name: 'Duplicate name',
            kind,
            sequence,
            marks: []
        }))
    };
    let job = await (await request('/api/alignments', 202, post(body))).json();
    assert.equal((await (await request('/api/alignments', 202, post(body))).json()).id, job.id);
    const deadline = Date.now() + 140000;
    while (['QUEUED', 'RUNNING'].includes(job.status) && Date.now() < deadline) {
        await new Promise(resolve => setTimeout(resolve, 300));
        job = await (await request(`/api/jobs/${job.id}`)).json();
    }
    assert.equal(job.status, 'SUCCEEDED', job.error);
    assert.equal(job.result.score, null);
    assert.equal(job.result.provenance.tool, 'T-Coffee');
    assert.deepEqual(job.result.sequences.map(row => row.value.replaceAll('-', '')), values);
    assert.equal(new Set(job.result.sequences.map(row => row.value.length)).size, 1);
    const alignment = job.result;
    const treeRows = alignment.sequences;
    await request('/api/trees', 400, post({
        requestId: crypto.randomUUID(), sourceAlignmentId: alignment.id,
        name: alignment.name, kind, sequences: treeRows.slice(0, 2)
    }));
    let treeJob = await (await request('/api/trees', 202, post({
        requestId: crypto.randomUUID(),
        sourceAlignmentId: alignment.id, name: alignment.name, kind, sequences: treeRows
    }))).json();
    const treeDeadline = Date.now() + 140000;
    while (['QUEUED', 'RUNNING'].includes(treeJob.status) && Date.now() < treeDeadline) {
        await new Promise(resolve => setTimeout(resolve, 300));
        treeJob = await (await request(`/api/jobs/${treeJob.id}`)).json();
    }
    assert.equal(treeJob.status, 'SUCCEEDED', treeJob.error);
    assert.equal(treeJob.result.method, 'tcoffee-nj-guide-v1');
    assert.equal(treeJob.result.nodes.filter(node => node.sourceIndex !== null).length, treeRows.length);
    const treeProject = {
        name: 'Guide tree smoke',
        sequences: [],
        alignments: [],
        trees: [treeJob.result],
        entryOrder: [treeJob.result.id]
    };
    const treeShare = await (await request('/api/projects', 200, post({project: treeProject}))).json();
    assert.deepEqual((await (await request(`/api/projects/${treeShare.shareKey}`)).json()).trees[0], treeJob.result);
}
assert.equal((await (await request('/api/tools/primer3')).json()).available, true, 'Primer3 thermodynamic probe failed');
const template = readFileSync(new URL('fixtures/primer3.txt', import.meta.url), 'utf8').split('\n').find(line => line.startsWith('SEQUENCE_TEMPLATE=')).split('=')[1];
for (const includeProbe of [false, true]) {
    const constraints = {minSize: 18, optSize: 20, maxSize: 25, minTm: 55, optTm: 60, maxTm: 65, minGc: 20, maxGc: 80};
    const body = {
        requestId: crypto.randomUUID(),
        sequence: {id: 'primer-smoke', name: 'Primer smoke', kind: 'DNA', sequence: template, marks: []},
        settings: {
            productMin: 100, productMax: 350, numReturn: 3, primers: constraints, includeProbe,
            probe: {...constraints, maxSize: 27, minTm: 60, optTm: 65, maxTm: 70}, target: {start: 99, end: 120},
            saltMonovalent: 50, saltDivalent: 1.5, dntp: 0.6, dnaConcentration: 50
        }
    };
    let job = await (await request('/api/primers', 202, post(body))).json();
    const deadline = Date.now() + 140000;
    while (['QUEUED', 'RUNNING'].includes(job.status) && Date.now() < deadline) {
        await new Promise(resolve => setTimeout(resolve, 300));
        job = await (await request(`/api/jobs/${job.id}`)).json();
    }
    assert.equal(job.status, 'SUCCEEDED', job.error);
    assert.ok(job.result.pairs.length > 0, 'No fixture primer pairs returned');
    for (const pair of job.result.pairs) {
        assert.equal(pair.productSize, pair.right.end - pair.left.start);
        const reverse = [...template.slice(pair.right.start, pair.right.end)].reverse().map(base => ({
            A: 'T',
            C: 'G',
            G: 'C',
            T: 'A'
        })[base]).join('');
        assert.equal(pair.right.sequence, reverse);
        assert.equal(Boolean(pair.probe), includeProbe);
        if (includeProbe) assert.equal(pair.probe.sequence, template.slice(pair.probe.start, pair.probe.end));
    }
    const project = {
        name: 'Primer persistence smoke',
        sequences: [],
        alignments: [],
        primerDesigns: [job.result],
        entryOrder: [job.result.id]
    };
    const saved = await (await request('/api/projects', 200, post({project}))).json();
    const loaded = await (await request(`/api/projects/${saved.shareKey}`)).json();
    assert.deepEqual(loaded.primerDesigns[0], job.result);
}
const imported = await (await request('/api/sequences/import', 200, post({text: '>Container smoke\nACGT'}))).json();
assert.equal(imported.sequences[0].sequence, 'ACGT');
const project = {
    name: 'Container persistence smoke',
    sequences: [{id: 'smoke-sequence', name: 'Container smoke', kind: 'DNA', sequence: 'ACGT', marks: []}],
    alignments: [], logos: [], entryOrder: ['smoke-sequence']
};
const shareKey = process.argv[2] ?? (await (await request('/api/projects', 200, post({project}))).json()).shareKey;
assert.ok(shareKey && /^[a-zA-Z0-9_-]+$/.test(shareKey));
const loaded = await (await request(`/api/projects/${shareKey}`)).json();
assert.equal(loaded.name, project.name);
assert.deepEqual(loaded.sequences, project.sequences);
assert.deepEqual(loaded.entryOrder, project.entryOrder);
console.log(JSON.stringify({status: 'passed', shareKey, base}));
