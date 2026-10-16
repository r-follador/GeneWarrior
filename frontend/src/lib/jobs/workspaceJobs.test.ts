import {expect, it} from 'vitest';
import {jobBusy, jobEntryId, workspaceOrder} from './workspaceJobs';
import type {WorkspaceJob} from '../stores/toolJobStore';

it('places placeholders immediately after their source without adding them to the project order', () => {
    const order = ['one', 'two'];
    const job: WorkspaceJob = {id: 'abc', sourceId: 'one', sequences: []};
    expect(workspaceOrder(order, [job])).toEqual(['one', jobEntryId(job), 'two']);
    expect(order).toEqual(['one', 'two']);
    expect(jobBusy(job)).toBe(true);
    expect(workspaceOrder(order, [{...job, sourceId: 'deleted'}])).toEqual(['one', 'two', jobEntryId(job)]);
});
it('removes placeholders once the stable result exists and retains failed jobs for retry', () => {
    const job: WorkspaceJob = {id: 'abc', sourceId: 'one', sequences: [], tool: 'PRIMER3'};
    expect(workspaceOrder(['one', 'primer3-abc', 'two'], [job])).toEqual(['one', 'primer3-abc', 'two']);
    expect(workspaceOrder(['one'], [{...job, applied: true}])).toEqual(['one']);
    expect(jobBusy({...job, abandoned: true})).toBe(false);
});
