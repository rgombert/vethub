<script lang="ts">
	import '../app.css';
	import { Toaster } from 'svelte-sonner';
	import { Header, Footer } from '$lib/components/layout';
	import { themeStore } from '$lib/theme/theme.svelte';

	let { children } = $props();

	const toasterTheme = $derived(themeStore.resolved === 'dark' ? 'dark' : 'light');
</script>

<Toaster position="top-right" richColors closeButton theme={toasterTheme} />

<div class="flex min-h-screen flex-col bg-background">
	<Header />

	<main class="flex-1">
		{@render children()}
	</main>

	<Footer />
</div>
