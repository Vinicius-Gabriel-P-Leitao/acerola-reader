import {
	clickText,
	exactTextSelector,
	firstDisplayed,
	getPathname,
	navigateTo,
	setStoreValue,
	waitForTauriReady,
	waitForText
} from '../helpers/app';
import { createReaderFixture } from '../helpers/fixtures';

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

	it('permite avançar por todos os passos do tutorial com pasta configurada até a conclusão', async () => {
		const fixture = createReaderFixture('Acerola Tutorial Fixture');

		await waitForTauriReady();
		await navigateTo('/home');
		await setStoreValue('onboarding_completed', false);
		await setStoreValue('library_path', fixture.rootDir);
		await browser.execute(() => window.location.reload());
		await waitForTauriReady();

		// Passo 0 -> Passo 1: Boas-vindas -> Idioma
		await clickText('Começar');

		await waitForText('Selecionar Idioma');
		await waitForText('Escolha o idioma preferido para a aplicação.');
		await waitForText('Voltar');
		await waitForText('Próximo');

		// Passo 1 -> Passo 2: Idioma -> Formatos
		await clickText('Próximo');

		await waitForText('Formatos Suportados');
		await waitForText('O Acerola suporta os seguintes formatos:');
		await waitForText('CBZ');
		await waitForText('CBR');
		await waitForText('PDF');

		// Passo 2 -> Passo 3: Formatos -> Configurações
		await clickText('Próximo');

		await waitForText('Configuração Inicial');
		await waitForText('Tema');
		await waitForText('Pasta dos Quadrinhos');

		// Com a pasta pré-configurada em settings.json, o botão "Próximo" está habilitado
		const nextBtn = await firstDisplayed(
			`${exactTextSelector('Próximo')}/ancestor-or-self::button[1]`
		);
		expect(await nextBtn.isEnabled()).toBe(true);

		// Passo 3 -> Passo 4: Configurações -> Tudo Pronto
		await nextBtn.click();

		await waitForText('Tudo Pronto!');
		await waitForText('Seu Acerola está pronto para uso.');
		await waitForText('Ir para Início');

		// Passo 4 -> Finalizar onboarding e ir para a Home
		await clickText('Ir para Início');

		// Valida navegação para a home principal do app
		await browser.waitUntil(async () => (await getPathname()) === '/home', {
			timeout: 5_000,
			interval: 100,
			timeoutMsg: 'App não navegou para /home após concluir o tutorial de onboarding.'
		});

		await waitForText('Início');
		await waitForText('Histórico');
		await waitForText('Configurações');
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
