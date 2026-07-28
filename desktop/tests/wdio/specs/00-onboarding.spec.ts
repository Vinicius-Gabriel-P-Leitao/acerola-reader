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

describe('onboarding e tutorial inicial', () => {
	it('renderiza o tutorial de primeira abertura ao abrir o app sem dados salvos', async () => {
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

	it('bloqueia o avanço no passo de configurações se nenhuma pasta estiver configurada', async () => {
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


	it('permite navegar para trás usando o botão Voltar', async () => {
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
