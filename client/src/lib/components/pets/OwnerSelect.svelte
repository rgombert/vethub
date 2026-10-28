<script lang="ts">
	import type { OwnerResponse } from '$lib/api/models';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import * as Select from '$lib/components/ui/select';
	import { Search } from 'lucide-svelte';

	interface Props {
		owners: OwnerResponse[];
		value?: number;
		disabled?: boolean;
	}

	let { owners, value = $bindable(), disabled = false }: Props = $props();

	let filter = $state('');

	let selectedOwner = $derived(owners.find((o) => o.id === value));
	let filteredOwners = $derived.by(() => {
		const query = filter.trim().toLowerCase();
		if (!query) return owners;
		return owners.filter(
			(o) =>
				o.firstName?.toLowerCase().includes(query) || o.lastName?.toLowerCase().includes(query)
		);
	});
</script>

<div class="space-y-2">
	<Label for="owner">Owner *</Label>
	<div class="relative">
		<Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
		<Input
			type="search"
			placeholder="Filter owners by first or last name..."
			bind:value={filter}
			class="pl-10"
			{disabled}
		/>
	</div>
	<Select.Root
		type="single"
		value={value?.toString()}
		onValueChange={(v) => (value = v ? Number(v) : undefined)}
	>
		<Select.Trigger id="owner" class="w-full" {disabled}>
			{selectedOwner ? `${selectedOwner.firstName} ${selectedOwner.lastName}` : 'Select an owner'}
		</Select.Trigger>
		<Select.Content>
			{#each filteredOwners as owner (owner.id)}
				<Select.Item value={owner.id.toString()}>
					{owner.firstName} {owner.lastName}
				</Select.Item>
			{:else}
				<p class="px-2 py-1.5 text-sm text-muted-foreground">No owners found</p>
			{/each}
		</Select.Content>
	</Select.Root>
	{#if !value}
		<p class="text-xs text-muted-foreground">Owner is required</p>
	{/if}
</div>
