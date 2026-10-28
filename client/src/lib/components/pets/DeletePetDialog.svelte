<script lang="ts">
	import type { PetResponse } from '$lib/api/models';
	import { Button } from '$lib/components/ui/button';
	import * as Dialog from '$lib/components/ui/dialog';
	import { Loader2 } from 'lucide-svelte';

	interface Props {
		open: boolean;
		pet: PetResponse | null;
		deleting?: boolean;
		onConfirm: () => void | Promise<void>;
	}

	let { open = $bindable(), pet, deleting = false, onConfirm }: Props = $props();
</script>

<Dialog.Root bind:open>
	<Dialog.Content>
		<Dialog.Header>
			<Dialog.Title>Delete pet</Dialog.Title>
			<Dialog.Description>
				Delete {pet?.name}? This also deletes all of {pet?.name}'s visits. This cannot be undone.
			</Dialog.Description>
		</Dialog.Header>
		<Dialog.Footer>
			<Button variant="outline" onclick={() => (open = false)} disabled={deleting}>Cancel</Button>
			<Button variant="destructive" onclick={onConfirm} disabled={deleting}>
				{#if deleting}
					<Loader2 class="mr-2 h-4 w-4 animate-spin" />
				{/if}
				Delete
			</Button>
		</Dialog.Footer>
	</Dialog.Content>
</Dialog.Root>
