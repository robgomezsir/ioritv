# IORITv Web App

Aplicação web para gestão de clientes e finanças do IORITv, construída com Next.js 15, React 19 e Firebase.

## Funcionalidades

- **Dashboard** - Visão geral com métricas e gráficos
- **Gestão de Clientes** - Cadastro, edição e acompanhamento de clientes
- **Financeiro** - Controle de pagamentos e créditos
- **Configurações** - Gerenciamento de temas e preferências
- **PWA** - Instalável como aplicativo no dispositivo

## Stack

- **Frontend:** Next.js 15, React 19, TypeScript
- **Estilização:** Tailwind CSS 4
- **Backend:** Firebase (Firestore, Auth)
- **Deploy:** Vercel (automático a cada push)

## Desenvolvimento

```bash
# Instalar dependências
npm install

# Rodar em desenvolvimento
npm run dev

# Build para produção
npm run build

# Iniciar servidor de produção
npm start
```

## Estrutura

```
web-app/
├── app/
│   ├── clientes/        # Gestão de clientes
│   │   └── [id]/        # Detalhes do cliente (rota dinâmica)
│   ├── configuracoes/   # Configurações do sistema
│   ├── dashboard/       # Painel principal
│   ├── financeiro/      # Gestão financeira
│   └── login/           # Autenticação
├── components/          # Componentes reutilizáveis
├── firebase/            # Configuração do Firebase
└── utils/               # Utilitários e helpers
```

## Deploy

O deploy é feito automaticamente via Vercel a cada push na branch `main`.

- **Produção:** [https://ioritv-70318.web.app](https://ioritv-70318.web.app)
- **Preview:** Gerado automaticamente em Pull Requests

## Licença

Projeto privado - Elevador Com
