<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { components } from '$lib/types/api';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import { Label } from '$lib/components/ui/label';
	import * as Select from '$lib/components/ui/select';
	import { Loader2 } from 'lucide-svelte';

	type PetType = components['schemas']['PetTypeResponse'];

	interface Props {
		name?: string;
		birthDate?: string;
		typeId?: number;
		petTypes: PetType[];
		onSubmit: (data: { name: string; birthDate: string; typeId: number }) => Promise<void>;
		submitLabel?: string;
		beforeFields?: Snippet;
		canSubmit?: boolean;
		onCancel?: () => void;
	}

	let {
		name: initialName = '',
		birthDate: initialBirthDate = '',
		typeId: initialTypeId,
		petTypes,
		onSubmit,
		submitLabel = 'Save',
		beforeFields,
		canSubmit = true,
		onCancel = () => history.back()
	}: Props = $props();

	let name = $state(initialName);
	let birthDate = $state(initialBirthDate);
	let selectedTypeId = $state<number | undefined>(initialTypeId);
	let submitting = $state(false);

	// Get selected type for display
	let selectedType = $derived(petTypes.find((t) => t.id === selectedTypeId));

	async function handleSubmit(e: Event) {
		e.preventDefault();
		if (!selectedTypeId || !canSubmit) {
			return;
		}
		submitting = true;
		try {
			await onSubmit({
				name: name.trim(),
				birthDate,
				typeId: selectedTypeId
			});
		} finally {
			submitting = false;
		}
	}
</script>

<form onsubmit={handleSubmit} class="space-y-6">
	{@render beforeFields?.()}

	<div class="space-y-2">
		<Label for="name">Pet Name</Label>
		<Input
			id="name"
			bind:value={name}
			placeholder="Enter pet name"
			required
			disabled={submitting}
		/>
	</div>

	<div class="space-y-2">
		<Label for="birthDate">Birth Date</Label>
		<Input
			id="birthDate"
			type="date"
			bind:value={birthDate}
			required
			disabled={submitting}
		/>
	</div>

	<div class="space-y-2">
		<Label for="petType">Pet Type</Label>
		<Select.Root 
			type="single"
			value={selectedTypeId?.toString()}
			onValueChange={(value) => selectedTypeId = value ? Number(value) : undefined}
		>
			<Select.Trigger id="petType" class="w-full" disabled={submitting}>
				{selectedType?.name || 'Select a pet type'}
			</Select.Trigger>
			<Select.Content>
				{#each petTypes as petType (petType.id)}
					<Select.Item value={petType.id.toString()}>
						{petType.name}
					</Select.Item>
				{/each}
			</Select.Content>
		</Select.Root>
	</div>

	<div class="flex justify-end gap-3">
		<Button type="button" variant="outline" onclick={onCancel} disabled={submitting}>
			Cancel
		</Button>
		<Button type="submit" disabled={submitting || !selectedTypeId || !canSubmit}>
			{#if submitting}
				<Loader2 class="mr-2 h-4 w-4 animate-spin" />
			{/if}
			{submitLabel}
		</Button>
	</div>
</form>
