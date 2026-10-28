<script lang="ts">
	import { goto } from '$app/navigation';
	import { createPet } from '$lib/api/pet/PetController';
	import { getPetTypes } from '$lib/api/pet-type/PetTypeController';
	import { getOwners } from '$lib/api/owner/OwnerController';
	import type { OwnerResponse, PetTypeResponse } from '$lib/api/models';
	import PetForm from '$lib/components/pets/PetForm.svelte';
	import OwnerSelect from '$lib/components/pets/OwnerSelect.svelte';
	import { Button } from '$lib/components/ui/button';
	import { ArrowLeft } from 'lucide-svelte';
	import { toast } from 'svelte-sonner';

	let owners = $state<OwnerResponse[]>([]);
	let petTypes = $state<PetTypeResponse[]>([]);
	let loading = $state(true);
	let loadFailed = $state(false);
	let ownerId = $state<number | undefined>();

	async function loadFormData() {
		loading = true;
		try {
			[owners, petTypes] = await Promise.all([getOwners(), getPetTypes()]);
		} catch (err) {
			loadFailed = true;
			toast.error('Failed to load form data');
			console.error('Error loading form data:', err);
		} finally {
			loading = false;
		}
	}

	async function handleSubmit(data: { name: string; birthDate: string; typeId: number }) {
		if (!ownerId) return;
		try {
			const pet = await createPet({ ownerId, ...data });
			toast.success('Pet added successfully');
			goto(`/owners/${ownerId}/pets/${pet.id}`);
		} catch (err) {
			toast.error('Failed to add pet');
			console.error('Error adding pet:', err);
		}
	}

	$effect(() => {
		loadFormData();
	});
</script>

<svelte:head>
	<title>Add pet | VetHub</title>
</svelte:head>

<div class="container mx-auto max-w-2xl px-4 py-8">
	<Button variant="ghost" href="/pets" class="mb-6 gap-2">
		<ArrowLeft class="h-4 w-4" />
		Back to pets
	</Button>

	<div class="mb-6">
		<h1 class="text-2xl font-bold">Add pet</h1>
		<p class="text-muted-foreground">Register a new pet for an existing owner</p>
	</div>

	{#if loading}
		<div class="card p-12 text-center">
			<div class="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
			<p class="text-muted-foreground">Loading form...</p>
		</div>
	{:else}
		<div class="card p-6">
			<PetForm
				{petTypes}
				onSubmit={handleSubmit}
				submitLabel="Add pet"
				canSubmit={!loadFailed && !!ownerId}
				onCancel={() => goto('/pets')}
			>
				{#snippet beforeFields()}
					<OwnerSelect {owners} bind:value={ownerId} disabled={loadFailed} />
				{/snippet}
			</PetForm>
		</div>
	{/if}
</div>
