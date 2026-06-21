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

### Com Docker Compose

1. **Configure o banco externo** (MySQL 8) e crie o database:
```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS arenagamer"
```

2. **Configure as variáveis de ambiente**:
```bash
cp env.example .env
# Edite .env com host, usuário e senha do seu banco externo
```

3. **Suba os serviços**:
```bash
docker compose up -d
```

Isso sobe Redis, RabbitMQ e a aplicação na porta `8080`. O MySQL **não** roda em Docker — a app conecta no banco definido em `DB_URL` no `.env`.

> **Dica**: se o MySQL estiver na mesma máquina que o Docker, use `host.docker.internal` como host na `DB_URL` (já é o padrão no `env.example`).

### Desenvolvimento Local

1. **Pré-requisitos**: Java 17, MySQL 8, Redis, RabbitMQ

2. **Banco de dados**:
```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS arenagamer"
```

3. **Configurar variáveis** (ou copiar `env.example`):
```bash
cp env.example .env
```

4. **Rodar**:
```bash
./mvnw spring-boot:run
```

5. **Swagger UI**: http://localhost:8080/swagger-ui.html

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

### Public (`/api/v1/public`)

**Sem autenticação:**
| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Registro |
| POST | `/auth/login` | Login |
| POST | `/auth/refresh` | Renovar token |

**HTTP Basic Auth:**
| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/plans` | Planos disponíveis |
| GET | `/tournaments` | Torneios públicos |
| GET | `/presets` | Presets de jogos |

### Common (`/api/v1/common`) — JWT

| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/auth/logout` | Logout |
| GET | `/users/me` | Perfil |
| PUT | `/users/me` | Atualizar perfil |
| DELETE | `/users/me` | Desativar conta |
| GET | `/wallet/balance` | Saldo |
| POST | `/wallet/deposit` | Depositar |
| POST | `/wallet/withdraw` | Sacar |
| GET | `/wallet/transactions` | Histórico |
| POST | `/teams` | Criar time |
| GET | `/teams/{id}` | Detalhes do time |
| GET | `/teams/my` | Meus times |
| POST | `/tournaments` | Criar torneio |
| GET | `/tournaments/my-created` | Meus criados |
| GET | `/tournaments/my-joined` | Meus inscritos |
| GET | `/tournaments/{slug}` | Detalhes |
| PUT | `/tournaments/{slug}/status` | Atualizar status |
| DELETE | `/tournaments/{slug}` | Cancelar |
| POST | `/tournaments/{slug}/participants` | Inscrever (solo) |
| POST | `/tournaments/{slug}/participants/team` | Inscrever time |
| DELETE | `/tournaments/{slug}/participants/{id}` | Expulsar |
| POST | `/tournaments/{slug}/generate-bracket` | Gerar chaves |
| GET | `/tournaments/{slug}/matches` | Listar partidas |
| POST | `/tournaments/{slug}/schedule` | Agendar partidas |
| PUT | `/tournaments/matches/{matchId}/reschedule` | Reagendar partida |

### Admin (`/api/v1/admin`) — JWT staff

| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/credit-tiers` | Tiers de créditos |
| GET | `/presets` | Presets (admin) |
| GET | `/audits` | Audit logs |
| GET | `/tournaments` | Todos os torneios |
| GET | `/users` | Staff |
| GET | `/contacts` | Contatos |
| GET/POST/PUT/DELETE | `/plans` | CRUD de planos |

## Tipos de Torneio

- **SINGLE_ELIMINATION**: Mata-mata com byes automáticos
- **ROUND_ROBIN**: Todos contra todos
- **GROUP_STAGE**: Fase de grupos com round-robin interno
- **DOUBLE_ELIMINATION**: *(futuro)*
- **SWISS**: *(futuro)*

## Modelo de Dados

Tabelas principais (prefixo `tbl`): `tblusers`, `tblclients`, `tblplans`, `tblcredit_tiers`, `tblwallets`, `tbltransactions`, `tbltournaments`, `tbltournament_participants`, `tblteams`, `tblteam_members`, `tblrounds`, `tblmatches`, `tblbracket_seeds`, `tblgroup_standings`, `tblavailability_profiles`, `tblpresets`, `tblpositions`, `tblaudit_logs`, `tblwebhook_subscriptions`, `tbloauth_clients`, `tblapi_keys`.

Veja a migration completa em `src/main/resources/db/migration/V1__initial_schema.sql`.
