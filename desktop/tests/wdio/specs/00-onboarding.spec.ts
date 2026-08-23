import {
	clickText,
	exactTextSelector,
	firstDisplayed,
	getPathname,
	getStoreValue,
	navigateTo,
	setStoreValue,
	setStoreValues,
	waitForTauriReady,
	waitForText
} from '../helpers/app';

describe('onboarding and initial tutorial', () => {
	it('renders first open tutorial when opening app without saved data', async () => {
		await waitForTauriReady();
		await navigateTo('/home');
		await setStoreValue('onboarding_completed', false);
		await setStoreValue('library_path', null);
		await browser.execute(() => window.location.reload());
		await waitForTauriReady();

		// Passo 0: Boas-vindas
		await waitForText('Acerola');
		await waitForText('Bem-vindo ao Acerola, seu leitor de quadrinhos.');
		await waitForText('Começar');
	});

	it('blocks advance in settings step if no folder is configured', async () => {
		await waitForTauriReady();
		await navigateTo('/home');
		await setStoreValue('onboarding_completed', false);
		await setStoreValue('library_path', null);
		await browser.execute(() => window.location.reload());
		await waitForTauriReady();

		// Avança até o Passo 3 (Configurações)
		await clickText('Começar');
		await waitForText('Selecionar Idioma');

		await clickText('Próximo');
		await waitForText('Formatos Suportados');

		await clickText('Próximo');
		await waitForText('Configuração Inicial');

		// Sem pasta de biblioteca em settings.json, o botão "Próximo" deve estar desabilitado
		const nextBtn = await firstDisplayed(
			`${exactTextSelector('Próximo')}/ancestor-or-self::button[1]`
		);
		expect(await nextBtn.isEnabled()).toBe(false);
	});

	it('allows navigating back using the Back button', async () => {
		await waitForTauriReady();
		await navigateTo('/home');
		await setStoreValue('onboarding_completed', false);
		await browser.execute(() => window.location.reload());
		await waitForTauriReady();

		// Avança até o Passo 2 (Formatos)
		await clickText('Começar');
		await waitForText('Selecionar Idioma');

		await clickText('Próximo');
		await waitForText('Formatos Suportados');

		// Volta para o Passo 1 (Idioma)
		await clickText('Voltar');
		await waitForText('Selecionar Idioma');

		// Volta para o Passo 0 (Boas-vindas)
		await clickText('Voltar');
		await waitForText('Bem-vindo ao Acerola, seu leitor de quadrinhos.');
	});
});
