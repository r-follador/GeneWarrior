import './styles.css';
import {mount} from 'svelte';
import App from './routes/App.svelte';

const app = mount(App, {
    target: document.getElementById('app') as HTMLElement
});

export default app;
