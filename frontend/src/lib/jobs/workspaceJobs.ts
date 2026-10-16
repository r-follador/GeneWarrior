import type {WorkspaceJob} from '../stores/toolJobStore';

export const jobEntryId = (job: WorkspaceJob) => 'job-' + job.id;
export const jobTitle = (job: WorkspaceJob) => job.tool === 'PRIMER3' ? 'Primer design' : job.tool === 'GUIDE_TREE' ? 'T-Coffee guide tree' : 'T-Coffee alignment';
export const jobIcon = (job: WorkspaceJob) => job.tool === 'PRIMER3' ? 'PCR' : job.tool === 'GUIDE_TREE' ? 'T' : 'A';
export const jobResultLabel = (job: WorkspaceJob) => job.tool === 'PRIMER3' ? 'primer design' : job.tool === 'GUIDE_TREE' ? 'guide tree' : 'alignment';
export const jobBusy = (job: WorkspaceJob) => !job.abandoned && (!job.job || ['QUEUED', 'RUNNING'].includes(job.job.status));

export function workspaceOrder(order: string[], jobs: WorkspaceJob[]) {
    const placeholders = jobs.filter(job => !job.applied && !order.includes((job.tool === 'PRIMER3' ? 'primer3-' : job.tool === 'GUIDE_TREE' ? 'tree-' : 'tcoffee-') + job.id));
    const result = order.flatMap(id => [id, ...placeholders.filter(job => job.sourceId === id).map(jobEntryId)]);
    return [...result, ...placeholders.filter(job => !order.includes(job.sourceId)).map(jobEntryId)];
}
