# SmartDesk - Coworking Platform

![SmartDesk Preview](preview.png)

**Progetto finale di tesi di laurea — Ingegneria del Software**  
Politecnico di Milano

| Nome     | Matricola |
| -------- | --------- |
| Cesandri | 248170    |
| Gheojan  | 247840    |

---

## Il progetto

SmartDesk è una piattaforma di coworking che mette in contatto chi vuole rendere disponibili i propri spazi con chi cerca un posto dove lavorare in tranquillità.

Chi gestisce una sede può pubblicare i propri uffici indicando la disponibilità di uno o più desk, gli orari di apertura e chiusura e le dotazioni presenti.

Chi cerca uno spazio può sfogliare le sedi, prenotare una postazione per la fascia oraria che preferisce e segnalare eventuali problemi tramite ticket di manutenzione.

---

## Stack

| Layer    | Tecnologie                                   |
| -------- | -------------------------------------------- |
| Frontend | Angular 21, TypeScript, Bootstrap 5          |
| Backend  | Java 17, Spring Boot 4, Spring Security, JWT |
| Database | MySQL                                        |
| Build    | Maven (backend), npm (frontend)              |

---

## Avvio

### Prerequisiti

- Java 17
- Node.js e npm
- MySQL in esecuzione, con un database configurato in `application.properties`

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Il server si avvia di default su `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
ng serve
```

L'applicazione è raggiungibile su `http://localhost:4200`.
