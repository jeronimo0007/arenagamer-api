# ArenaGamer API

Plataforma de gerenciamento de torneios de e-sports e jogos competitivos.

## Stack

- **Java 17** + **Spring Boot 3.5**
- **Spring Security** (JWT)
- **Spring Data JPA** (Hibernate) + MySQL 8
- **Spring Cache** (Redis)
- **RabbitMQ** para processamento assíncrono
- **Flyway** para migrações de banco
- **SpringDoc OpenAPI** (Swagger UI)
- **Docker Compose** para ambiente local

## Setup Rápido

A API **cria o banco e aplica o schema automaticamente** via Flyway ao subir — funciona com **XAMPP (MySQL local)** ou **Docker**. Você só precisa apontar `DB_URL` no `.env`.

```bash
cp .env.example .env
```

### Sem Docker (XAMPP + API na IDE)

1. **Pré-requisitos**: Java 17, MySQL no XAMPP (porta **3306**), Redis e RabbitMQ rodando localmente
2. No `.env`, use a linha padrão (`127.0.0.1:3306`, `DB_PASSWORD` vazio se o root do XAMPP não tiver senha)
3. Suba Redis e RabbitMQ (instalação local ou só esses serviços no Docker):
   ```bash
   docker compose up -d redis rabbitmq
   ```
4. Rode `ArenaGamerApplication` na IDE ou `./mvnw spring-boot:run`
5. **Swagger UI**: http://localhost:8080/swagger-ui.html

> Use o Tomcat **embutido** da Spring Boot (porta **8080**). O Tomcat do XAMPP é outro servidor — não é necessário para esta API.

### Com Docker Compose (tudo containerizado)

1. Ajuste o `.env` para a linha comentada da porta **3307** (`DB_PASSWORD=root`)
2. Suba todos os serviços:
   ```bash
   docker compose up -d
   ```
3. API em http://localhost:8080/swagger-ui.html

### Desenvolvimento híbrido (API local + infra Docker)

1. No `.env`, MySQL na **3307** (Docker) ou **3306** (XAMPP) — conforme sua escolha
2. Suba só a infraestrutura:
   ```bash
   docker compose up -d mysql redis rabbitmq
   ```
3. Rode a API pela IDE ou `./mvnw spring-boot:run`

A app carrega o `.env` automaticamente ao rodar pela IDE ou `mvnw spring-boot:run`.

### Banco de dados (automático)

| Cenário | O que acontece ao subir a API |
|---------|-------------------------------|
| Banco `arenagamer` vazio | Flyway cria tabelas Perfex mínimas (V1_1), tabelas Arena (V2+) e seeds |
| Banco Perfex já existente | V1_1 não altera tabelas existentes; V2+ cria só o que falta e aplica ALTERs |
| Migração falhou no meio | Veja [Recuperar migração Flyway](#recuperar-migração-flyway) abaixo |

### Pré-requisitos de serviços

| Serviço | Porta padrão | XAMPP / local | Docker |
|---------|--------------|---------------|--------|
| MySQL 8 | 3306 (XAMPP) ou 3307 (Docker) | Painel XAMPP | `docker compose up -d mysql` |
| Redis | 6379 | Instalação local ou Docker | `docker compose up -d redis` |
| RabbitMQ | 5672 | Instalação local ou Docker | `docker compose up -d rabbitmq` |

Teste o MySQL Docker:
```bash
docker exec arenagamer-mysql mysql -uroot -proot -e "SELECT 1"
```

### Erro `Communications link failure` / `entityManagerFactory`

MySQL inacessível — confira `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no `.env` e se o MySQL está rodando.

### Recuperar migração Flyway

Se aparecer `Detected failed migration to version X`:

```sql
-- No MySQL afetado (phpMyAdmin ou cliente SQL)
DELETE FROM flyway_schema_history WHERE success = 0;
-- Remova tabelas parciais da migração que falhou, se existirem
```

Ou recrie o banco do zero em desenvolvimento:

```sql
DROP DATABASE arenagamer;
CREATE DATABASE arenagamer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Depois suba a API novamente.

## Estrutura do Projeto

```
src/main/java/com/arenagamer/api/
├── config/          # SecurityConfig, RabbitMQ, OpenAPI
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities + enums
├── exception/       # Global exception handler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, utilities
└── service/         # Business logic
```

## Endpoints

A API está organizada em três prefixos:

| Prefixo | Auth | Público |
|---------|------|---------|
| `/api/v1/public` | Sem auth (auth) ou Basic (catálogo) | Leitura e login |
| `/api/v1/common` | JWT Bearer | Staff e clientes |
| `/api/v1/admin` | JWT Bearer (staff) | Painel administrativo |

Documentação interativa completa: **Swagger UI** em `/swagger-ui.html` (88 endpoints).

### Public (`/api/v1/public`)

**Sem autenticação:**
| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Registrar novo cliente |
| POST | `/auth/login` | Login (staff ou cliente) |
| POST | `/auth/refresh` | Renovar token de acesso |

**HTTP Basic Auth:**
| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/plans` | Listar planos disponíveis |
| GET | `/tournaments` | Listar torneios públicos |
| GET | `/presets` | Listar presets de jogos |
| GET | `/tournament-pricing` | Obter preços para criação de torneios |
| GET | `/team-settings` | Obter limites de times |
| GET | `/teams` | Listar times públicos (com ranks) |
| GET | `/teams/{id}` | Detalhes de time público |
| GET | `/teams/ranks/global?presetId=` | Ranking global por jogo |
| GET | `/teams/ranks/regional?presetId=&state=` | Ranking regional por jogo |

### Common (`/api/v1/common`) — JWT Bearer

| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/auth/logout` | Encerrar sessão |
| GET | `/users/me` | Obter perfil |
| PUT | `/users/me` | Atualizar perfil |
| DELETE | `/users/me` | Desativar conta |
| GET | `/wallet/balance` | Consultar saldo da empresa |
| POST | `/wallet/deposit` | Depositar créditos |
| POST | `/wallet/withdraw` | Sacar créditos |
| GET | `/wallet/transactions` | Histórico de transações |
| GET | `/wallet/permissions` | Listar permissões de créditos dos contatos |
| PUT | `/wallet/permissions/{contactId}` | Atualizar permissões de um contato |
| POST | `/teams` | Criar time (contato primário; dono = cliente) |
| GET | `/teams/manageable` | Times que posso gerenciar (primário) |
| GET | `/teams/my` | Times em que meu cliente participa |
| GET | `/teams/ranks/me?presetId=` | Meu rank (global/regional por jogo) |
| PUT | `/teams/{id}` | Atualizar time |
| DELETE | `/teams/{id}` | Excluir time |
| GET | `/teams/{id}/manage` | Painel: time + membros + canManage |
| GET | `/teams/{id}/members` | Listar clientes membros |
| GET | `/teams/{id}` | Detalhes do time |
| POST | `/teams/{teamId}/members/clients/{clientUserId}` | Adicionar cliente |
| DELETE | `/teams/{teamId}/members/clients/{clientUserId}` | Remover cliente |
| POST | `/teams/{teamId}/transfer/clients/{newClientUserId}` | Transferir para outro cliente |

\* Excluir bloqueado se inscrito em torneio. Gerenciar/transferir: contato primário do dono.

| POST | `/tournaments` | Criar torneio |
| PUT | `/tournaments/{slug}` | Atualizar torneio |
| GET | `/tournaments/my-created` | Meus torneios criados |
| GET | `/tournaments/my-managed` | Torneios que posso gerenciar |
| GET | `/tournaments/my-joined` | Torneios que participo |
| GET | `/tournaments/{slug}` | Detalhes do torneio |
| PUT | `/tournaments/{slug}/status` | Atualizar status |
| DELETE | `/tournaments/{slug}` | Cancelar torneio |
| POST | `/tournaments/{slug}/participants` | Inscrever-se (solo) |
| POST | `/tournaments/{slug}/participants/team` | Inscrever time |
| DELETE | `/tournaments/{slug}/participants/{participantId}` | Expulsar participante |
| POST | `/tournaments/{slug}/generate-bracket` | Gerar chaves |
| GET | `/tournaments/{slug}/matches` | Listar partidas |
| POST | `/tournaments/{slug}/schedule` | Agendar partidas |
| PUT | `/tournaments/matches/{matchId}/reschedule` | Reagendar partida |
| GET | `/tournaments/{slug}/managers` | Listar gestores do torneio |
| POST | `/tournaments/{slug}/managers/{contactId}` | Conceder permissão de gestão |
| DELETE | `/tournaments/{slug}/managers/{contactId}` | Revogar permissão de gestão |
| POST | `/subscriptions` | Contratar, upgrade ou agendar downgrade |
| POST | `/subscriptions/with-credits` | Contratar/upgrade debitando créditos |
| DELETE | `/subscriptions` | Agendar cancelamento do plano pago |

### Admin (`/api/v1/admin`) — JWT Bearer (staff)

| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/audits` | Listar audit logs |
| POST | `/audits` | Registrar auditoria manual |
| GET | `/tournaments` | Listar todos os torneios |
| GET | `/users` | Listar staff |
| GET | `/contacts` | Listar contatos de clientes |
| GET | `/presets` | Listar presets de jogos |
| GET | `/presets/{id}` | Detalhes do preset |
| POST | `/presets` | Criar preset |
| PUT | `/presets/{id}` | Atualizar preset |
| GET | `/team-settings` | Obter configurações de times |
| PUT | `/team-settings` | Atualizar configurações de times |
| GET | `/wallet/client/{clientUserId}` | Carteira do cliente |
| GET | `/wallet/client/{clientUserId}/transactions` | Histórico de créditos do cliente |
| POST | `/wallet/client/{clientUserId}/deposit` | Adicionar créditos ao cliente |
| POST | `/wallet/client/{clientUserId}/withdraw` | Remover créditos do cliente |
| GET | `/subscriptions` | Listar assinaturas ativas |
| GET | `/subscriptions/plan/{planId}` | Listar assinantes de um plano |
| GET | `/subscriptions/client/{clientUserId}` | Plano ativo de um cliente |
| PUT | `/subscriptions/client/{clientUserId}` | Atribuir ou trocar plano |
| DELETE | `/subscriptions/client/{clientUserId}` | Remover plano ativo |
| POST | `/subscriptions/client/{clientUserId}/reset-usage` | Resetar contagem mensal |
| GET | `/plans` | Listar planos |
| GET | `/plans/{id}` | Detalhes do plano |
| POST | `/plans` | Criar plano |
| PUT | `/plans/{id}` | Atualizar plano |
| DELETE | `/plans/{id}` | Remover plano |
| GET | `/credit-tiers` | Listar tiers de créditos |
| GET | `/credit-tiers/{id}` | Detalhes do tier |
| POST | `/credit-tiers` | Criar tier de créditos |
| PUT | `/credit-tiers/{id}` | Atualizar tier de créditos |
| DELETE | `/credit-tiers/{id}` | Remover tier de créditos |
| GET | `/tournament-pricing` | Obter configuração de preços de torneio |
| PUT | `/tournament-pricing` | Atualizar configuração de preços de torneio |

## Tipos de Torneio

- **SINGLE_ELIMINATION**: Mata-mata com byes automáticos
- **ROUND_ROBIN**: Todos contra todos
- **GROUP_STAGE**: Fase de grupos com round-robin interno
- **DOUBLE_ELIMINATION**: *(futuro)*
- **SWISS**: *(futuro)*

## Modelo de Dados

Tabelas principais (prefixo `tbl`): `tblusers`, `tblclients`, `tblplans`, `tblcredit_tiers`, `tblwallets`, `tbltransactions`, `tbltournaments`, `tbltournament_participants`, `tblteams`, `tblteam_members`, `tblrounds`, `tblmatches`, `tblbracket_seeds`, `tblgroup_standings`, `tblavailability_profiles`, `tblpresets`, `tblpositions`, `tblaudit_logs`, `tblwebhook_subscriptions`, `tbloauth_clients`, `tblapi_keys`.

Veja as migrations em `src/main/resources/db/migration/` (V1_1 Perfex mínimo, V2+ Arena).
