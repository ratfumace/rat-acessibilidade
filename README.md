# 🛡️ Rat Acessibilidade — Guardião Anti-Queda de Acessibilidade

Aplicativo Android nativo, ultraleve, 100% código aberto e autônomo, projetado para manter serviços de acessibilidade essenciais sempre operacionais 24 horas por dia, 7 dias por semana.

Se o Android desligar qualquer app monitorado por economia de bateria, fechamento agressivo de processos em segundo plano pelo fabricante (Xiaomi, Samsung, Motorola, etc.) ou reinicialização do celular, o Guardião **reativa instantaneamente** sem intervenção manual.

---

## 🔒 Auditoria de Segurança & Privacidade (Open Source)

Este aplicativo foi desenvolvido para ser totalmente auditável por qualquer desenvolvedor ou entusiasta de segurança:

- **Zero Telemetria:** Não envia nenhum dado para servidores externos.
- **Zero Anúncios:** Não possui SDKs de publicidade ou rastreadores.
- **Zero Root Necessário:** Utiliza apenas a permissão oficial do sistema Android `WRITE_SECURE_SETTINGS` concedida via ADB.
- **Código Transparente:**
  - [`A11yWatchdogService.java`](app/src/main/java/com/rat/acessibilidade/A11yWatchdogService.java): Serviço de primeiro plano (*Foreground Service*) ultraleve com *wakelock* gerenciado que faz as rondas periódicas de vigilância.
  - [`A11yManager.java`](app/src/main/java/com/rat/acessibilidade/A11yManager.java): Executa a leitura e gravação segura na tabela `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.
  - [`MainActivity.java`](app/src/main/java/com/rat/acessibilidade/MainActivity.java): Interface gráfica moderna em abas para seleção de apps e ações de um toque.

---

## 🚀 Principais Recursos

1. **Escolha Totalmente Livre de Apps**:
   - Você escolhe exatamente quais aplicativos quer proteger em um seletor visual com busca rápida.
   - Lista principal exibe apenas os aplicativos sob sua proteção.

2. **⚡ Ações Rápidas de Um Toque**:
   - **LIGAR TODAS AS ACESSIBILIDADES:** Ativa todos os serviços instalados no celular de uma vez.
   - **DESLIGAR TODAS AS ACESSIBILIDADES:** Desativa todas as acessibilidades em lote.
   - **RELIGAR APPS MARCADOS:** Força a ativação imediata dos apps protegidos.
   - **REINICIAR SERVIÇOS (1s):** Pausa por 1 segundo e religa as acessibilidades para destravar conexões congeladas pelo Android.

3. **🤖 Didática Pronta para Leigos (Copiar Prompt para IA)**:
   - Botão de 1 toque que copia um prompt formatado para enviar ao ChatGPT, Gemini, Claude ou DeepSeek, permitindo que qualquer amigo configure o ADB no PC sem complicação.

4. **🎥 Vídeo Tutorial do YouTube Integrado**:
   - Botão direto para assistir o vídeo explicativo de como instalar o ADB no PC e conectar o celular via USB.

5. **⏰ Frequência de Checagem & Horário Fixo Diário**:
   - Intervalo configurável: *10s, 30s, 1m, 5m, 15m, 1 hora*.
   - Horário fixo com alarme integrado ao kernel (`AlarmManager` com `RTC_WAKEUP`): acorda o aparelho de madrugada (ex: **⏰ 05:00**) e renova todas as conexões travadas.

---

## 💻 Permissão Necessária via Computador (ADB)

O aplicativo precisa apenas de uma permissão especial do Android para conseguir religar os serviços em segundo plano:

```bash
adb shell pm grant com.rat.acessibilidade android.permission.WRITE_SECURE_SETTINGS
```

---

## 🛠️ Como Compilar por Conta Própria (Build from Source)

### Requisitos:
- **JDK 17** ou superior instalado (`java -version`).
- **Android SDK** configurado (ou Android Studio instalado).

### Passos de Compilação:

1. Clone o repositório:
```bash
git clone https://github.com/ratfumace/rat-acessibilidade.git
cd rat-acessibilidade
```

2. Compile a versão Release:
```bash
./gradlew assembleRelease
```
*(No Windows, utilize `gradlew.bat assembleRelease`)*

3. O APK compilado e assinado estará disponível em:
```
RatAcessibilidade.apk  (na raiz do projeto)
# ou em:
app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Instalação Direta via ADB

Para instalar diretamente no celular conectado via USB ou Wi-Fi:

```bash
python3 deploy.py
# ou manualmente:
adb install -r RatAcessibilidade.apk
adb shell pm grant com.rat.acessibilidade android.permission.WRITE_SECURE_SETTINGS
```

---

## 📄 Licença
Código livre sob a licença MIT. Sinta-se livre para usar, auditar, modificar e distribuir.
