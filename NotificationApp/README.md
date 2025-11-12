# Aplicativo de Notificações Android

Este é um aplicativo Android simples que permite criar notificações personalizadas na barra de notificações do dispositivo.

## 📱 Funcionalidades

- Campo de texto para digitar mensagens personalizadas
- Botão para enviar notificações
- Suporte para Android 7.0 (API 24) até as versões mais recentes
- Gerenciamento automático de permissões (Android 13+)
- Interface limpa e intuitiva

## 🚀 Como Usar

1. **Abra o aplicativo**
2. **Digite sua mensagem** na caixa de texto
3. **Clique no botão "Enviar Notificação"**
4. **Permita notificações** quando solicitado (apenas na primeira vez no Android 13+)
5. **Veja sua notificação** aparecer na barra de notificações!

## 🛠️ Como Importar no Android Studio

1. **Abra o Android Studio**
2. Clique em **File → Open**
3. Navegue até a pasta `NotificationApp`
4. Clique em **OK**
5. Aguarde o Gradle sincronizar o projeto
6. Clique no botão **Run** (▶️) ou pressione **Shift + F10**

## 📋 Requisitos

- **Android Studio** Arctic Fox ou superior
- **SDK mínimo:** Android 7.0 (API 24)
- **SDK alvo:** Android 14 (API 34)
- **Kotlin** 1.9.20

## 🔑 Permissões

O aplicativo solicita a seguinte permissão:
- **POST_NOTIFICATIONS** - Necessária para enviar notificações no Android 13 (API 33) ou superior

## 📂 Estrutura do Projeto

```
NotificationApp/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/notificationapp/
│   │       │   └── MainActivity.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml
│   │       │   └── values/
│   │       │       ├── strings.xml
│   │       │       ├── colors.xml
│   │       │       └── themes.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 💡 Características Técnicas

- **Linguagem:** Kotlin
- **UI:** ConstraintLayout
- **Notificações:** NotificationCompat API
- **Canais de Notificação:** Implementados para Android 8.0+
- **Permissões em tempo de execução:** Implementadas para Android 13+

## 🎨 Interface

A interface é simples e inclui:
- Título do aplicativo
- Instruções claras
- Campo de entrada de texto
- Botão de ação
- Ícone decorativo

## 🔧 Personalização

Você pode personalizar:
- **Cores:** Edite `colors.xml`
- **Textos:** Edite `strings.xml`
- **Layout:** Edite `activity_main.xml`
- **Ícone da notificação:** Altere em `MainActivity.kt` na linha `setSmallIcon()`

## 📝 Notas

- Cada notificação recebe um ID único baseado no timestamp
- As notificações são automaticamente canceláveis (auto-cancel)
- O campo de texto é limpo automaticamente após enviar a notificação
- Toast messages informam o status das ações

## 🐛 Solução de Problemas

**Notificações não aparecem:**
- Verifique se você concedeu permissão para notificações
- Vá em Configurações → Apps → Notificações → Permissões
- Certifique-se de que as notificações não estão bloqueadas

**Erro de compilação:**
- Execute `Build → Clean Project`
- Execute `Build → Rebuild Project`
- Sincronize o Gradle: `File → Sync Project with Gradle Files`

## 📄 Licença

Este projeto é de código aberto e está disponível para uso educacional.
