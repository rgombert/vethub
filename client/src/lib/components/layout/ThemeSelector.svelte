<script lang="ts">
	import { Sun, Moon, Monitor, PawPrint } from 'lucide-svelte';
	import { Button } from '$lib/components/ui/button';
	import * as DropdownMenu from '$lib/components/ui/dropdown-menu';
	import { themeStore, type Theme } from '$lib/theme/theme.svelte';

	let { showLabel = false }: { showLabel?: boolean } = $props();

	const options = [
		{ value: 'light', label: 'Light', icon: Sun },
		{ value: 'dark', label: 'Dark', icon: Moon },
		{ value: 'system', label: 'System', icon: Monitor },
		{ value: 'pastel', label: 'Pastel Paws', icon: PawPrint }
	] as const;

	const current = $derived(options.find((o) => o.value === themeStore.theme) ?? options[2]);
</script>

<DropdownMenu.Root>
	<DropdownMenu.Trigger>
		{#snippet child({ props })}
			<Button
				{...props}
				variant="ghost"
				size={showLabel ? 'default' : 'icon'}
				aria-label="Change theme (current: {current.label})"
			>
				<current.icon class="h-5 w-5" />
				{#if showLabel}
					<span>Theme: {current.label}</span>
				{/if}
			</Button>
		{/snippet}
	</DropdownMenu.Trigger>
	<DropdownMenu.Content align="end" class="w-44">
		<DropdownMenu.Label>Theme</DropdownMenu.Label>
		<DropdownMenu.Separator />
		<DropdownMenu.RadioGroup
			value={themeStore.theme}
			onValueChange={(value) => themeStore.setTheme(value as Theme)}
		>
			{#each options as option (option.value)}
				<DropdownMenu.RadioItem value={option.value}>
					<option.icon class="h-4 w-4" />
					{option.label}
				</DropdownMenu.RadioItem>
			{/each}
		</DropdownMenu.RadioGroup>
	</DropdownMenu.Content>
</DropdownMenu.Root>
