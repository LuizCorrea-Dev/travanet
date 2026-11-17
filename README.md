# TravaNet — Controle Parental de Wi-Fi e Internet Móvel

**TravaNet** é um aplicativo Android em Kotlin e Jetpack Compose projetado para controle parental de conectividade no próprio dispositivo, permitindo bloquear o tráfego de **Wi-Fi** e **internet móvel (dados móveis)** por meio de senha dos pais, bloqueios rápidos, bloqueio total e agendamentos por dias e horários.

---

## Principais Funcionalidades

- **Firewall Local Sem Root (`VpnService`)**
  - Intercepta e descarta localmente o tráfego de rede quando um bloqueio está ativo, bloqueando Wi-Fi, internet móvel ou ambos diretamente no dispositivo.
  - Monitoramento em tempo real via `ConnectivityManager` para identificar se a conexão ativa é Wi-Fi ou rede celular e aplicar regras seletivas.
- **Proteção por Senha Parental (PIN)**
  - Criação obrigatória de PIN de 4 dígitos no primeiro acesso, armazenado com **hash SHA-256 + salt aleatório** em `DataStore` Preferences.
  - **Desbloqueio imediato** a qualquer momento mediante validação da senha dos pais.
  - **Trava Anti-Desligamento:** impede desativar perfis, editar regras ativas, pausar a proteção ou alterar o PIN sem a senha.
- **Bloqueio Rápido e Bloqueio Total**
  - Ações rápidas de **30 min**, **1 hora** e **2 horas** com contagem regressiva ao vivo.
  - **Bloqueio Total:** corta a conexão por tempo indeterminado até que a senha parental seja inserida.
  - Seletor de alcance: **Wi-Fi + Móvel**, **Só Wi-Fi** ou **Só Móvel**.
- **Perfis de Bloqueio e Agendamentos**
  - Cadastro completo de perfis personalizados (ex.: *Hora de Dormir*, *Foco nos Estudos*) com persistência local em **Room**.
  - Dois modos de regra:
    - **Bloquear neste horário:** bloqueia a internet na janela definida.
    - **Uso permitido:** libera a internet apenas dentro da janela definida e bloqueia fora dela.
  - Seleção por dias da semana (`Seg–Dom`) e horários com suporte a janelas noturnas (ex.: `22:00` às `07:00`).
- **Ação de Pausa e Proteção contra Desinstalação**
  - **Pausar Proteção:** opções de pausa temporária (`15 min`, `30 min`, `1h`, `2h`) protegidas pela senha dos pais, com retomada automática ao término do tempo.
  - **Blindagem contra Desinstalação (`DeviceAdminReceiver`):** impede a desinstalação direta pelas configurações/launcher sem autorização; tentativas de desativação nas configurações do sistema acionam aviso parental e bloqueio total preventivo.
  - Fluxo de desinstalação assistida na aba **Segurança**, exigindo o PIN dos pais.

---

## Arquitetura e Tecnologias

- **Linguagem & UI:** Kotlin, Jetpack Compose, Material Design 3 (`enableEdgeToEdge`, layout adaptativo com `NavigationBar` e `NavigationRail`), fontes locais *Plus Jakarta Sans* e *JetBrains Mono*.
- **Persistência Local:**
  - **Room (`TravaNetDatabase`)**: entidades `BlockingProfileEntity` e `ScheduleRuleEntity`.
  - **DataStore Preferences (`ParentalPrefsStore`)**: hash do PIN, estado de bloqueio total, temporizadores de bloqueio rápido/pausa e preferências de trava.
- **Serviços e Agendamento em Segundo Plano:**
  - `ParentalVpnService`: serviço `VpnService` em primeiro plano (`foregroundServiceType="specialUse"`).
  - `ScheduleAlarmManager` + `ScheduleReceiver`: transições precisas de início/fim de bloqueios e pausas.
  - `BootReceiver`: restauração automática de bloqueios e alarmes após reinicialização ou alteração de hora/fuso horário.
  - `ParentalDeviceAdminReceiver`: proteção opcional contra desinstalação.

---

## Estrutura do Projeto

```text
app/src/main/
├── java/com/example/
│   ├── MainActivity.kt                  # Navegação Compose, permissões e fluxos de UI
│   ├── TravaNetApplication.kt           # Inicialização do repositório e sincronização de bloqueio
│   ├── data/
│   │   ├── TravaNetRepository.kt        # Camada única de acesso a Room e DataStore
│   │   ├── db/                          # Entidades, DAO e banco Room
│   │   └── prefs/                       # Armazenamento seguro de PIN e estado em DataStore
│   ├── domain/
│   │   └── LockStateEvaluator.kt        # Motor puro de avaliação de regras, dias e horários
│   ├── service/
│   │   ├── ParentalVpnService.kt        # Firewall local VpnService
│   │   ├── NetworkMonitor.kt            # Monitor de conectividade Wi-Fi / Celular
│   │   ├── LockCoordinator.kt           # Sincronização entre regras, alarmes e VPN
│   │   ├── ScheduleAlarmManager.kt      # Agendamento de transições no AlarmManager
│   │   ├── ScheduleReceiver.kt          # Receptor de alarmes de horário
│   │   ├── BootReceiver.kt              # Restauração pós-boot e mudança de horário
│   │   └── ParentalDeviceAdminReceiver.kt
│   └── ui/
│       ├── TravaNetViewModel.kt         # Gerenciamento de estado e ações protegidas por PIN
│       ├── components/                  # Teclado PIN, Blindagem Inicial, Pausa e Regras
│       ├── screens/                     # Painel, Perfis, Editor de Perfil e Segurança
│       └── theme/                       # Cores, tipografia e formas Material 3
└── res/                                 # Ícone adaptativo, fontes, strings e XMLs de sistema
```

---

## Permissões Utilizadas

| Permissão | Finalidade |
| --- | --- |
| `BIND_VPN_SERVICE` | Operar o firewall local para bloquear tráfego Wi-Fi e móvel sem root. |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Identificar se a rede ativa é Wi-Fi ou dados móveis. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Manter o serviço de bloqueio ativo em segundo plano enquanto houver restrição vigente. |
| `POST_NOTIFICATIONS` | Exibir o status persistente do bloqueio ativo (Android 13+). |
| `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM` | Reaplicar regras após reiniciar o aparelho e disparar agendamentos no horário configurado. |
| `BIND_DEVICE_ADMIN` *(opcional)* | Impedir a desinstalação não autorizada do aplicativo. |

---

## Como Compilar e Testar

```bash
# Compilar APK Debug
gradle :app:assembleDebug

# Executar testes unitários e Robolectric
gradle :app:testDebugUnitTest
```

---

## Política de Privacidade

O TravaNet opera integralmente de forma local no dispositivo, sem coleta, venda ou envio de dados para servidores externos. Consulte a página completa em [`privacy-policy.html`](https://htmlpreview.github.io/?https://github.com/LuizCorrea-Dev/travanet/blob/main/privacy-policy.html).
