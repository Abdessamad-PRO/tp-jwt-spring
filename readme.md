# TP – Sécurisation d'une API REST avec Spring Security et JWT

> Module : Sécurité des systèmes — Master SDIA (Systèmes Distribués et Intelligence Artificielle)
> ENSET Mohammedia — Université Hassan II de Casablanca

## 1. Objectif du TP

Mettre en place une API REST **sécurisée** avec **Spring Boot**, **Spring Security** et **JSON Web Token (JWT)**. L'application permet de :

- créer un compte utilisateur (`/auth/register`) avec mot de passe haché en **BCrypt** ;
- s'authentifier et recevoir un **token JWT** (`/auth/login`) ;
- accéder à des endpoints protégés (`/api/**`) en envoyant ce token dans l'en-tête `Authorization: Bearer <token>`.

L'authentification est **stateless** : aucune session côté serveur, chaque requête est validée grâce au token.

## 2. Technologies utilisées

| Technologie | Rôle |
|---|---|
| Java 17+ | Langage |
| Spring Boot 3.x | Framework applicatif |
| Spring Web | API REST |
| Spring Security 6 | Authentification et autorisation |
| Spring Data JPA | Accès aux données |
| H2 (en mémoire) | Base de données de test |
| JWT (génération/validation) | Token d'authentification |
| BCrypt | Hachage des mots de passe |
| Maven | Gestion des dépendances |
| Postman | Tests des endpoints |

## 3. Architecture du projet

```
System_Security.tp_jwt_spring
├── config
│   └── SecurityConfig.java            # Configuration Spring Security (filtres, règles d'accès)
├── Controller
│   ├── AuthController.java            # /auth/register et /auth/login
│   └── UserController.java            # /api/hello et /api/profile (protégés)
├── dtos
│   ├── AuthResponse.java              # Réponse contenant le token
│   ├── LoginRequest.java              # email + password
│   └── RegisterRequest.java           # email + password
├── entities
│   └── AppUser.java                   # Entité utilisateur (table USERS)
├── repositories
│   └── UserRepository.java            # Accès BD (existsByEmail, findByEmail...)
├── security
│   └── JwtFilter.java                 # Filtre qui valide le token à chaque requête
├── services
│   ├── CustomerUserDetailsService.java # Charge l'utilisateur pour Spring Security
│   └── JwtService.java                # Génération / extraction / validation du JWT
└── TpJwtSpringApplication.java        # Point d'entrée
```

## 4. Fonctionnement de l'authentification JWT

```
 Client                                   Serveur
   |  1. POST /auth/register                 |
   |---------------------------------------->|  mot de passe haché (BCrypt) puis sauvegarde
   |  2. POST /auth/login {email, password}  |
   |---------------------------------------->|  AuthenticationManager vérifie les identifiants
   |  <------ { "token": "eyJhbGci..." } ----|  JwtService génère le token
   |  3. GET /api/profile                    |
   |     Authorization: Bearer <token>       |
   |---------------------------------------->|  JwtFilter valide le token
   |  <------ 200 OK + réponse --------------|  puis SecurityContext est renseigné
```

### Rôle de chaque composant

- **`SecurityConfig`** : désactive CSRF (API stateless), impose `SessionCreationPolicy.STATELESS`, déclare les règles d'accès et insère `JwtFilter` avant `UsernamePasswordAuthenticationFilter`. Fournit aussi les beans `PasswordEncoder` (BCrypt) et `AuthenticationManager`.
- **`JwtFilter`** (`OncePerRequestFilter`) : lit l'en-tête `Authorization`, extrait le token après `Bearer `, récupère l'utilisateur, vérifie la validité du token puis place l'authentification dans le `SecurityContextHolder`.
- **`JwtService`** : génère le token (algorithme **HS256**, subject = email de l'utilisateur, date d'émission et d'expiration), extrait le username et vérifie la validité.
- **`CustomerUserDetailsService`** : implémente `UserDetailsService` pour charger l'utilisateur depuis la base à partir de son email.
- **`AuthController`** : inscription et connexion.
- **`UserController`** : endpoints protégés, accessibles uniquement avec un token valide.

### Règles d'accès configurées

| Route | Accès |
|---|---|
| `/auth/**` | Public |
| `/h2-console/**` | Public (console H2, développement uniquement) |
| `/api/**` | **Authentifié** (token JWT requis) |
| Autres routes | Public |

## 5. Endpoints de l'API

| Méthode | URL | Protégé | Description |
|---|---|---|---|
| POST | `/auth/register` | Non | Crée un utilisateur (rôle `USER`) |
| POST | `/auth/login` | Non | Authentifie l'utilisateur et retourne un JWT |
| GET | `/api/hello` | Oui | Message de bienvenue |
| GET | `/api/profile` | Oui | Retourne l'utilisateur connecté |

### 5.1 `POST /auth/register`

**Corps de la requête (JSON)**

```json
{
  "email": "email@enset.ma",
  "password": "password123"
}
```

**Réponse** : `200 OK`

```
Utilisateur cree
```

Si l'email existe déjà : `400 Bad Request` avec le message `Email deja utilise`.

### 5.2 `POST /auth/login`

**Corps de la requête (JSON)**

```json
{
  "email": "email@enset.ma",
  "password": "password123"
}
```

**Réponse** : `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlbWFpbEBlbnNldC5tYSIsImlhdCI6...."
}
```

Le token est composé de trois parties : `header.payload.signature`. Le payload contient notamment le **subject** (email), la date d'émission (`iat`) et la date d'expiration (`exp`).

### 5.3 `GET /api/hello`

**En-tête requis** : `Authorization: Bearer <token>`

**Réponse** : `200 OK`

```
Bienvenue dans l'API securisee
```

### 5.4 `GET /api/profile`

**En-tête requis** : `Authorization: Bearer <token>`

**Réponse** : `200 OK`

```
Utilisateur connecte : email@enset.ma
```

## 6. Lancer le projet

### Prérequis

- JDK 17 ou supérieur
- Maven (ou le wrapper `mvnw` fourni)
- Postman (ou `curl`)

### Démarrage

```bash
# Cloner le projet
git clone <url-du-depot>
cd tp_jwt_spring

# Lancer l'application
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

L'application démarre sur **http://localhost:8080**.

### Console H2

- URL : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:security-db`
- Requête de vérification :

```sql
SELECT * FROM USERS;
```

La base étant **en mémoire**, les données sont perdues à chaque redémarrage.

## 7. Scénario de test (Postman)

### Étape 1 : Créer un utilisateur

- **POST** `http://localhost:8080/auth/register`
- Body → `raw` → `JSON` :

```json
{ "email": "email@enset.ma", "password": "password123" }
```

- Résultat attendu : `200 OK` – `Utilisateur cree`

![Register](docs/images/01-register.png)

### Étape 2 : Vérifier l'utilisateur en base (mot de passe haché)

La console H2 montre que le mot de passe n'est **pas stocké en clair** mais haché avec BCrypt (préfixe `$2a$10$...`), avec le rôle `USER`.

![H2 Console](docs/images/02-h2-users.png)

### Étape 3 : Se connecter et récupérer le token

- **POST** `http://localhost:8080/auth/login`
- Même corps JSON que l'inscription
- Résultat attendu : `200 OK` avec un objet `{ "token": "..." }`

![Login](docs/images/03-login.png)

### Étape 4 : Appeler `/api/hello` avec le token

- **GET** `http://localhost:8080/api/hello`
- Onglet **Authorization** → Type **Bearer Token** → coller le token
- Résultat attendu : `200 OK` – `Bienvenue dans l'API securisee`

![Hello](docs/images/04-hello.png)

### Étape 5 : Appeler `/api/profile` avec le token

- **GET** `http://localhost:8080/api/profile`
- Même configuration Bearer Token
- Résultat attendu : `200 OK` – `Utilisateur connecte : email@enset.ma`

![Profile](docs/images/05-profile.png)

### Étape 6 : Tester sans token (cas négatif)

- **GET** `http://localhost:8080/api/hello` **sans** en-tête `Authorization`
- Résultat attendu : accès refusé (`403 Forbidden`), car la route `/api/**` exige une authentification.

### Test équivalent avec `curl`

```bash
# 1. Inscription
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"email@enset.ma","password":"password123"}'

# 2. Connexion (récupérer le token)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"email@enset.ma","password":"password123"}'

# 3. Appel protégé
curl http://localhost:8080/api/profile \
  -H "Authorization: Bearer <TOKEN>"
```

## 8. Points de sécurité mis en œuvre

- **Mots de passe hachés** avec BCrypt (jamais stockés en clair).
- **Authentification stateless** : pas de session HTTP, chaque requête porte son token.
- **Token signé** (HS256) : toute modification du token invalide la signature.
- **Filtre JWT personnalisé** exécuté avant le filtre d'authentification standard.
- **Séparation des routes** publiques (`/auth/**`) et protégées (`/api/**`).

## 9. Limites et améliorations possibles

- Utiliser une base persistante (PostgreSQL, MySQL) au lieu de H2 en mémoire.
- Externaliser la clé secrète JWT et sa durée de validité dans `application.properties` ou des variables d'environnement.
- Ajouter la validation des entrées (`@Valid`, contraintes sur email et mot de passe).
- Gérer proprement les erreurs de token (expiré, invalide) avec des réponses `401` explicites au lieu d'ignorer l'exception dans `JwtFilter`.
- Exploiter les rôles (`USER`, `ADMIN`) avec `@PreAuthorize` ou `hasRole(...)`.
- Ajouter un mécanisme de **refresh token**.
- Désactiver la console H2 et restreindre CSRF/CORS selon l'environnement de production.
- Ajouter des tests unitaires et d'intégration (`MockMvc`).

## 10. Auteur

**Abdessamad**
Master Systèmes Distribués et Intelligence Artificielle (SDIA)
ENSET Mohammedia — Université Hassan II de Casablanca
