import type {OligoConstraints, PrimerDesignDto, PrimerOligo, PrimerSettings} from '../api/types';
import {reverseComplement} from './model';

function requireValue(condition: unknown, message: string): asserts condition {
    if (!condition) throw new Error(message);
}

function object(value: unknown): Record<string, unknown> {
    requireValue(value !== null && typeof value === 'object' && !Array.isArray(value), 'Invalid primer data');
    return value as Record<string, unknown>;
}

function text(value: unknown, max = 255) {
    requireValue(typeof value === 'string' && value.trim().length > 0 && value.length <= max, 'Invalid primer text');
    return value;
}

function id(value: unknown) {
    const result = text(value, 160);
    requireValue(/^[a-zA-Z0-9_-]+$/.test(result), 'Invalid primer entry ID');
    return result;
}

function number(value: unknown, min: number, max: number, integer = false) {
    requireValue(typeof value === 'number' && Number.isFinite(value) && value >= min && value <= max
        && (!integer || Number.isInteger(value)), 'Invalid primer numeric value');
    return value;
}

function constraints(value: unknown): OligoConstraints {
    const v = object(value);
    const minSize = number(v.minSize, 15, 35, true), optSize = number(v.optSize, minSize, 35, true),
        maxSize = number(v.maxSize, optSize, 35, true);
    const minTm = number(v.minTm, 30, 90), optTm = number(v.optTm, minTm, 90), maxTm = number(v.maxTm, optTm, 90);
    const minGc = number(v.minGc, 0, 100), maxGc = number(v.maxGc, minGc, 100);
    return {minSize, optSize, maxSize, minTm, optTm, maxTm, minGc, maxGc};
}

export function parsePrimerSettings(value: unknown, length: number): PrimerSettings {
    const v = object(value);
    const productMin = number(v.productMin, 40, length, true),
        productMax = number(v.productMax, productMin, 20000, true);
    let target = null;
    if (v.target != null) {
        const range = object(v.target), start = number(range.start, 0, length - 1, true),
            end = number(range.end, start + 1, length, true);
        target = {start, end};
    }
    requireValue(typeof v.includeProbe === 'boolean', 'Invalid probe option');
    return {
        productMin,
        productMax,
        target,
        includeProbe: v.includeProbe,
        numReturn: number(v.numReturn, 1, 10, true),
        primers: constraints(v.primers),
        probe: constraints(v.probe),
        saltMonovalent: number(v.saltMonovalent, 0.1, 500),
        saltDivalent: number(v.saltDivalent, 0, 100),
        dntp: number(v.dntp, 0, 100),
        dnaConcentration: number(v.dnaConcentration, 0.1, 1000)
    };
}

function oligo(value: unknown, template: string, reverse: boolean): PrimerOligo {
    const v = object(value), sequence = text(v.sequence, 35);
    const start = number(v.start, 0, template.length - 1, true), end = number(v.end, start + 1, template.length, true);
    const strand = reverse ? 'REVERSE' : 'FORWARD';
    const source = template.slice(start, end);
    requireValue(/^[ACGT]{15,35}$/.test(sequence) && sequence.length === end - start && /^[ACGT]+$/.test(source)
        && v.strand === strand && sequence === (reverse ? reverseComplement(source) : source), 'Primer does not match its template');
    return {
        sequence,
        start,
        end,
        strand,
        tm: number(v.tm, -100, 150),
        gc: number(v.gc, 0, 100),
        penalty: number(v.penalty, 0, 1000000)
    };
}

export function parsePrimerDesign(value: unknown): PrimerDesignDto {
    const v = object(value), rawSource = object(v.source);
    const sequence = text(rawSource.sequence, 20000);
    requireValue(rawSource.kind === 'DNA' && /^[ACGTN]{40,20000}$/.test(sequence), 'Invalid primer template');
    const source = {id: id(rawSource.id), name: text(rawSource.name), kind: 'DNA' as const, sequence, marks: []};
    const settings = parsePrimerSettings(v.settings, sequence.length);
    const version = text(v.version, 100);
    requireValue(/^[0-9.]+$/.test(version), 'Invalid Primer3 version');
    requireValue(Array.isArray(v.pairs) && v.pairs.length <= settings.numReturn, 'Invalid primer pairs');
    const pairs = v.pairs.map((item, index) => {
        const pair = object(item);
        requireValue(pair.index === index, 'Invalid primer pair order');
        const left = oligo(pair.left, sequence, false), right = oligo(pair.right, sequence, true);
        const probe = pair.probe == null ? null : oligo(pair.probe, sequence, false);
        const productSize = number(pair.productSize, settings.productMin, settings.productMax, true);
        requireValue(left.end <= right.start && productSize === right.end - left.start, 'Invalid product coordinates');
        requireValue(!settings.target || (left.end <= settings.target.start && right.start >= settings.target.end), 'Primers do not flank the target');
        requireValue(settings.includeProbe === Boolean(probe) && (!probe || (probe.start >= left.end && probe.end <= right.start)), 'Invalid probe location');
        return {index, left, right, probe, productSize, penalty: number(pair.penalty, 0, 1000000)};
    });
    requireValue(Array.isArray(v.explanations) && v.explanations.length <= 4, 'Invalid primer explanations');
    return {
        id: id(v.id),
        name: text(v.name),
        version,
        source,
        settings,
        pairs,
        explanations: v.explanations.map(item => text(item, 1000))
    };
}
