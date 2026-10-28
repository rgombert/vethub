<script lang="ts">
	import { getPets, deletePet } from '$lib/api/pet/PetController';
	import { getPetTypes } from '$lib/api/pet-type/PetTypeController';
	import type { PetResponse, PetTypeResponse } from '$lib/api/models';
	import DeletePetDialog from '$lib/components/pets/DeletePetDialog.svelte';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';
	import * as Select from '$lib/components/ui/select';
	import * as Table from '$lib/components/ui/table';
	import { PawPrint, Plus, Search } from 'lucide-svelte';
	import { toast } from 'svelte-sonner';

	const ALL_TYPES = 'all';

	let pets = $state<PetResponse[]>([]);
	let petTypes = $state<PetTypeResponse[]>([]);
	let loading = $state(true);
	let searchQuery = $state('');
	let typeFilter = $state(ALL_TYPES);

	let petToDelete = $state<PetResponse | null>(null);
	let deleteDialogOpen = $state(false);
	let deleting = $state(false);

	let filtersActive = $derived(searchQuery.trim() !== '' || typeFilter !== ALL_TYPES);
	let selectedTypeName = $derived(
		typeFilter === ALL_TYPES
			? 'All types'
			: (petTypes.find((t) => t.id.toString() === typeFilter)?.name ?? 'All types')
	);

	let filteredPets = $derived.by(() => {
		const query = searchQuery.trim().toLowerCase();
		return pets.filter((pet) => {
			const matchesText =
				!query ||
				pet.name?.toLowerCase().includes(query) ||
				pet.owner?.lastName?.toLowerCase().includes(query);
			const matchesType = typeFilter === ALL_TYPES || pet.type?.id?.toString() === typeFilter;
			return matchesText && matchesType;
		});
	});

	async function loadData() {
		loading = true;
		const [petsResult, typesResult] = await Promise.allSettled([getPets(), getPetTypes()]);
		if (petsResult.status === 'fulfilled') {
			pets = petsResult.value;
		} else {
			toast.error('Failed to load pets');
			console.error('Error loading pets:', petsResult.reason);
		}
		if (typesResult.status === 'fulfilled') {
			petTypes = typesResult.value;
		} else {
			console.error('Error loading pet types:', typesResult.reason);
		}
		loading = false;
	}

	function clearFilters() {
		searchQuery = '';
		typeFilter = ALL_TYPES;
	}

	function askDelete(pet: PetResponse) {
		petToDelete = pet;
		deleteDialogOpen = true;
	}

	async function confirmDelete() {
		if (!petToDelete) return;
		deleting = true;
		const id = petToDelete.id;
		try {
			await deletePet(id);
			pets = pets.filter((p) => p.id !== id);
			toast.success('Pet deleted');
		} catch (err) {
			toast.error('Failed to delete pet');
			console.error('Error deleting pet:', err);
		} finally {
			deleting = false;
			deleteDialogOpen = false;
		}
	}

	$effect(() => {
		loadData();
	});
</script>

<svelte:head>
	<title>Pets | VetHub</title>
</svelte:head>

<div class="container mx-auto px-4 py-8">
	<div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
		<div class="flex items-center gap-3">
			<div class="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
				<PawPrint class="h-6 w-6 text-primary" />
			</div>
			<div>
				<h1 class="text-2xl font-bold text-foreground">Pets</h1>
				<p class="text-sm text-muted-foreground">Find patients across all owners</p>
			</div>
		</div>
		<Button href="/pets/new" class="gap-2">
			<Plus class="h-4 w-4" />
			Add pet
		</Button>
	</div>

	<div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
		<div class="relative w-full sm:max-w-md">
			<Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
			<Input
				type="search"
				placeholder="Search pet or owner last name..."
				bind:value={searchQuery}
				class="pl-10"
			/>
		</div>
		{#if petTypes.length > 0}
			<Select.Root type="single" bind:value={typeFilter}>
				<Select.Trigger class="w-full sm:w-48" aria-label="Pet type">
					{selectedTypeName}
				</Select.Trigger>
				<Select.Content>
					<Select.Item value={ALL_TYPES}>All types</Select.Item>
					{#each petTypes as petType (petType.id)}
						<Select.Item value={petType.id.toString()}>{petType.name}</Select.Item>
					{/each}
				</Select.Content>
			</Select.Root>
		{/if}
		<Button variant="outline" onclick={clearFilters} disabled={!filtersActive}>Clear</Button>
	</div>

	{#if loading}
		<div class="card p-12 text-center">
			<div class="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
			<p class="text-muted-foreground">Loading pets...</p>
		</div>
	{:else if filteredPets.length === 0}
		<div class="card p-12 text-center">
			<PawPrint class="mx-auto mb-4 h-12 w-12 text-muted-foreground/50" />
			{#if filtersActive}
				<p class="text-muted-foreground">No pets found matching your filters</p>
			{:else}
				<p class="text-muted-foreground">No pets registered yet</p>
			{/if}
		</div>
	{:else}
		<div class="card overflow-hidden">
			<Table.Root>
				<Table.Header>
					<Table.Row>
						<Table.Head>Name</Table.Head>
						<Table.Head>Type</Table.Head>
						<Table.Head>Birth date</Table.Head>
						<Table.Head>Owner</Table.Head>
						<Table.Head>Actions</Table.Head>
					</Table.Row>
				</Table.Header>
				<Table.Body>
					{#each filteredPets as pet (pet.id)}
						<Table.Row class="hover:bg-muted/50">
							<Table.Cell>
								<a
									href="/owners/{pet.ownerId}/pets/{pet.id}"
									class="font-medium text-foreground hover:text-primary"
								>
									{pet.name}
								</a>
							</Table.Cell>
							<Table.Cell>{pet.type?.name}</Table.Cell>
							<Table.Cell>{pet.birthDate}</Table.Cell>
							<Table.Cell>
								<a href="/owners/{pet.ownerId}" class="hover:text-primary">
									{pet.owner?.firstName} {pet.owner?.lastName}
								</a>
							</Table.Cell>
							<Table.Cell>
								<div class="flex flex-wrap gap-1">
									<Button variant="ghost" size="sm" href="/owners/{pet.ownerId}/pets/{pet.id}">
										View
									</Button>
									<Button
										variant="ghost"
										size="sm"
										href="/owners/{pet.ownerId}/pets/{pet.id}/visits/new"
									>
										Add visit
									</Button>
									<Button
										variant="ghost"
										size="sm"
										class="text-destructive hover:text-destructive"
										onclick={() => askDelete(pet)}
									>
										Delete
									</Button>
								</div>
							</Table.Cell>
						</Table.Row>
					{/each}
				</Table.Body>
			</Table.Root>
		</div>
		<p class="mt-4 text-sm text-muted-foreground">
			Showing {filteredPets.length} of {pets.length} pets
		</p>
	{/if}
</div>

<DeletePetDialog bind:open={deleteDialogOpen} pet={petToDelete} {deleting} onConfirm={confirmDelete} />
