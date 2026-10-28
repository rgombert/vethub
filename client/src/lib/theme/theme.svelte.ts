import { browser } from '$app/environment';

export type Theme = 'light' | 'dark' | 'system' | 'pastel';
export type ResolvedTheme = 'light' | 'dark' | 'pastel';

// Keep in sync with the inline script in app.html
export const THEME_STORAGE_KEY = 'vethub-theme';
export const THEMES: readonly Theme[] = ['light', 'dark', 'system', 'pastel'];

const META_COLORS: Record<ResolvedTheme, string> = {
	light: '#E8003D',
	dark: '#14161B',
	pastel: '#F3EFFC'
};

const DARK_QUERY = '(prefers-color-scheme: dark)';

function isTheme(value: unknown): value is Theme {
	return typeof value === 'string' && (THEMES as readonly string[]).includes(value);
}

function readStoredTheme(): Theme {
	try {
		const stored = localStorage.getItem(THEME_STORAGE_KEY);
		return isTheme(stored) ? stored : 'system';
	} catch {
		return 'system';
	}
}

function writeStoredTheme(theme: Theme): void {
	try {
		localStorage.setItem(THEME_STORAGE_KEY, theme);
	} catch {
		// Storage unavailable (e.g. private mode): keep the choice for this session only
	}
}

function systemPrefersDark(): boolean {
	return browser && window.matchMedia(DARK_QUERY).matches;
}

class ThemeStore {
	theme = $state<Theme>(browser ? readStoredTheme() : 'system');
	private systemDark = $state(systemPrefersDark());

	resolved = $derived<ResolvedTheme>(
		this.theme === 'system' ? (this.systemDark ? 'dark' : 'light') : this.theme
	);

	constructor() {
		if (browser) {
			window.matchMedia(DARK_QUERY).addEventListener('change', (event) => {
				this.systemDark = event.matches;
				this.apply();
			});
			this.apply();
		}
	}

	setTheme(theme: Theme): void {
		this.theme = theme;
		writeStoredTheme(theme);
		this.apply();
	}

	apply(): void {
		if (!browser) return;
		const root = document.documentElement;
		const resolved = this.resolved;
		root.classList.toggle('dark', resolved === 'dark');
		if (resolved === 'pastel') {
			root.dataset.theme = 'pastel';
		} else {
			delete root.dataset.theme;
		}
		document.querySelector('meta[name="theme-color"]')?.setAttribute('content', META_COLORS[resolved]);
	}
}

export const themeStore = new ThemeStore();
