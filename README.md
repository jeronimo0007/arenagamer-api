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

```bash
docker compose up -d
```

Isso sobe MySQL, Redis, RabbitMQ e a aplicação na porta `8080`.

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

## Endpoints Principais

### Auth (`/api/v1/auth`)
| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/register` | Registro |
| POST | `/login` | Login |
| POST | `/refresh` | Renovar token |
| POST | `/logout` | Logout |

### Users (`/api/v1/users`)
| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/me` | Perfil |
| PUT | `/me` | Atualizar perfil |
| DELETE | `/me` | Desativar conta |

### Wallet (`/api/v1/wallet`)
| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/balance` | Saldo |
| POST | `/deposit` | Depositar |
| POST | `/withdraw` | Sacar |
| GET | `/transactions` | Histórico |

### Tournaments (`/api/v1/tournaments`)
| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/` | Criar torneio |
| GET | `/` | Listar públicos |
| GET | `/my-created` | Meus criados |
| GET | `/my-joined` | Meus inscritos |
| GET | `/{slug}` | Detalhes |
| PUT | `/{slug}/status` | Atualizar status |
| DELETE | `/{slug}` | Cancelar |
| POST | `/{slug}/participants` | Inscrever (solo) |
| POST | `/{slug}/participants/team` | Inscrever time |
| DELETE | `/{slug}/participants/{id}` | Expulsar |
| POST | `/{slug}/generate-bracket` | Gerar chaves |
| GET | `/{slug}/matches` | Listar partidas |
| POST | `/{slug}/schedule` | Agendar partidas |

### Teams (`/api/v1/teams`)
| Method | Path | Descrição |
|--------|------|-----------|
| POST | `/` | Criar time |
| GET | `/{id}` | Detalhes |
| GET | `/my` | Meus times |
| POST | `/{id}/members/{userId}` | Adicionar membro |
| DELETE | `/{id}/members/{userId}` | Remover membro |
| POST | `/{id}/transfer/{newOwnerId}` | Transferir liderança |

### Admin (`/api/v1/admin`) — requer role ADMIN
| Method | Path | Descrição |
|--------|------|-----------|
| GET | `/plans` | Planos |
| GET | `/credit-tiers` | Tiers de créditos |
| GET | `/presets` | Presets de jogos |
| GET | `/audits` | Audit logs |
| GET | `/tournaments` | Todos os torneios |
| GET | `/users` | Todos os usuários |

## Tipos de Torneio

- **SINGLE_ELIMINATION**: Mata-mata com byes automáticos
- **ROUND_ROBIN**: Todos contra todos
- **GROUP_STAGE**: Fase de grupos com round-robin interno
- **DOUBLE_ELIMINATION**: *(futuro)*
- **SWISS**: *(futuro)*

## Modelo de Dados

Tabelas principais: `users`, `clients`, `plans`, `credit_tiers`, `wallets`, `transactions`, `tournaments`, `tournament_participants`, `teams`, `team_members`, `rounds`, `matches`, `bracket_seeds`, `group_standings`, `availability_profiles`, `presets`, `positions`, `audit_logs`, `webhook_subscriptions`, `oauth_clients`, `api_keys`.

Veja a migration completa em `src/main/resources/db/migration/V1__initial_schema.sql`.
